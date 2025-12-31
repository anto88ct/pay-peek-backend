package com.paypeek.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.mapper.YearFolderMapper;
import com.paypeek.backend.dto.PayslipResponseDto;
import com.paypeek.backend.model.*;
import com.paypeek.backend.repository.PayrollTemplateRepository;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.repository.YearFolderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
@Slf4j
public class PayslipService {

    private final PayrollTemplateRepository payrollTemplateRepository;
    private final PayslipRepository payslipRepository;
    private final MinIOService minIOService;
    private final YearFolderRepository yearFolderRepository;
    private final UserRepository userRepository;
    private final YearFolderMapper yearFolderMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PayslipService self;

//    @Value("${app.extractor.url:http://paypeek-extractor:8000}") //PROD

    @Value("${app.extractor.url:http://localhost:8000}") //DEV
    private String extractorUrl;

    @Value("${app.lightrag.url:http://paypeek-lightrag:8020}")
    private String lightragUrl;

    public PayslipService(MinIOService minIOService,
                          YearFolderRepository yearFolderRepository,
                          UserRepository userRepository,
                          YearFolderMapper yearFolderMapper,
                          PayrollTemplateRepository payrollTemplateRepository,
                          PayslipRepository payslipRepository,
                          @Lazy PayslipService self,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper) { // <-- AGGIUNTO QUI
        this.minIOService = minIOService;
        this.yearFolderRepository = yearFolderRepository;
        this.userRepository = userRepository;
        this.yearFolderMapper = yearFolderMapper;
        this.payrollTemplateRepository = payrollTemplateRepository;
        this.payslipRepository = payslipRepository;
        this.self = self;
        this.objectMapper = objectMapper; // <-- ORA FUNZIONA
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(300))
                .build();
    }


    /**
     * CORE LOGIC: Crea un nuovo Template e la prima Payslip dinamica
     */
    public Payslip buildPayslipTemplate(MultipartFile file) {
        User user = getCurrentUser(); // Recupera l'utente
        log.info("Inizio buildPayslipTemplate per utente: {} - file: {}", user.getEmail(), file.getOriginalFilename());

        Map<String, Object> response = callPythonExtractor(file);

        if (response == null || !response.containsKey("signature")) {
            throw new RuntimeException("L'estrattore non ha restituito dati validi");
        }

        String signature = (String) response.get("signature");
        String nomeAzienda = (String) response.get("azienda");

        // Cerchiamo un template che abbia STESSA signature E STESSO userId
        PayrollTemplate template = payrollTemplateRepository.findBySignatureAndUserId(signature, user.getId())
                .orElseGet(() -> {
                    log.info("Creazione nuovo template per utente {} con signature {}.", user.getId(), signature);
                    Map<String, String> regexPatterns = (Map<String, String>) response.get("regex");

                    return payrollTemplateRepository.save(PayrollTemplate.builder()
                            .name("Template " + nomeAzienda)
                            .signature(signature)
                            .userId(user.getId()) // <--- Associamo l'utente
                            .regexPatterns(regexPatterns)
                            .build());
                });

        Map<String, Object> extractedData = (Map<String, Object>) response.get("extracted_data");

        // Salviamo la Payslip
        Payslip payslip = Payslip.builder()
                .templateId(template.getId())
                .fileName(file.getOriginalFilename())
                .extractedData(extractedData)
                .build();

        return payslipRepository.save(payslip);
    }



    /**
     * UPLOAD SINGOLO: Carica in una cartella specifica (ID mese fornito dal FE)
     */
    public FileItemDto uploadFile(String monthFolderId, MultipartFile file) {
        User user = getCurrentUser();

        // 1. Estrazione testo per LightRAG
        String markdown = extractMarkdown(file);

        if(markdown == null)
            throw new NullPointerException("Estrazione dati fallita");

        // 2. Upload su MinIO
        String minioFilename = uploadToMinio(file, String.valueOf(user.getId()));

        // 3. Ricerca della cartella di destinazione su MongoDB
        YearFolder yearFolder = yearFolderRepository.findAll().stream()
                .filter(yf -> yf.getUserId().equals(user.getId()))
                .filter(yf -> yf.getMonths().stream().anyMatch(m -> m.getId().equals(monthFolderId)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cartella Anno non trovata per il mese: " + monthFolderId));

        MonthFolder monthFolder = yearFolder.getMonths().stream()
                .filter(m -> m.getId().equals(monthFolderId))
                .findFirst().orElseThrow();

        // 4. Creazione FileItem
        FileItem fileItem = buildFileItem(file, minioFilename);
        monthFolder.getFiles().add(fileItem);
        yearFolderRepository.save(yearFolder);

        // 5. Pipeline AI asincrona
        if (markdown != null) {
            self.sendToLightRagAsync(file.getOriginalFilename(), markdown);
        }

        return yearFolderMapper.toDto(fileItem);
    }

    private int parseAiYear(Map<String, Object> aiData, String filename) {
        Object yearObj = aiData.get("anno"); // Supponendo che Python lo estragga
        try {
            if (yearObj != null && !yearObj.toString().equals("N.D.")) {
                return Integer.parseInt(yearObj.toString().replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {}
        // Fallback: cerca l'anno nel nome del file o usa quello corrente
        return LocalDate.now().getYear();
    }

    private int parseAiMonth(Map<String, Object> aiData) {
        // Simile a sopra per il mese
        return LocalDate.now().getMonthValue();
    }

    /**
     * MASS UPLOAD: Rileva automaticamente Anno/Mese dal contenuto del PDF
     */

    /**
     * MASS UPLOAD: Rileva automaticamente se usare Template (Regex) o AI Vision pura
     */
    public List<PayslipResponseDto> massUpload(List<MultipartFile> files) {
        User user = getCurrentUser();
        List<PayslipResponseDto> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 1. Calcolo Signature locale
                String signature = calculateSHA256(file);

                // 2. Controllo esistenza Template
                Optional<PayrollTemplate> templateOpt = payrollTemplateRepository.findBySignatureAndUserId(signature, user.getId());

                Map<String, Object> aiResponse;
                Map<String, Object> extractedData;

                if (templateOpt.isPresent()) {
                    log.info("Documento noto. Uso estrazione guidata per: {}", file.getOriginalFilename());
                    aiResponse = callPythonWithTemplate(file, templateOpt.get().getRegexPatterns());
                } else {
                    log.info("Documento nuovo. Uso AI Vision generica per: {}", file.getOriginalFilename());
                    aiResponse = callPythonExtractor(file);
                }

                // Estraiamo la mappa dei dati usando la chiave camelCase definita in Python
                extractedData = (Map<String, Object>) aiResponse.get("extractedData");

                if (extractedData == null) {
                    log.error("L'estrattore non ha restituito dati validi per {}", file.getOriginalFilename());
                    continue;
                }

                // 3. Determiniamo Anno e Mese (metodi aggiornati per navigare la mappa annidata)
                int year = parseYear(extractedData, file.getOriginalFilename());
                int month = parseMonth(extractedData);

                // 4. Upload Fisico su MinIO
                String minioUrl = uploadToMinio(file, user.getId());

                // 5. Salvataggio record Payslip su MongoDB (per persistenza e audit)
                Payslip payslip = Payslip.builder()
                        .userId(user.getId())
                        .templateId(templateOpt.map(PayrollTemplate::getId).orElse("AUTO_GENERATED"))
                        .fileName(file.getOriginalFilename())
                        .extractedData(extractedData)
                        .build();
                payslipRepository.save(payslip);

                // 6. Organizzazione Folder MongoDB (per la UI "stile cartelle")
                YearFolder yearFolder = getOrCreateYearFolder(user.getId(), year);
                MonthFolder monthFolder = getOrCreateMonthFolder(yearFolder, month);

                FileItem fileItem = FileItem.builder()
                        .id(UUID.randomUUID().toString())
                        .name(file.getOriginalFilename())
                        .url(minioUrl)
                        .type("pdf")
                        .size(file.getSize())
                        .uploadDate(Instant.now())
                        .anno(year)
                        .mese(month)
                        .dati(extractedData)
                        .build();

                monthFolder.getFiles().add(fileItem);
                yearFolderRepository.save(yearFolder);

                // 7. Prepariamo il DTO di risposta allineato
                results.add(PayslipResponseDto.builder()
                        .signature(signature)
                        .azienda((String) aiResponse.getOrDefault("azienda", "Sconosciuta"))
                        .regex((Map<String, String>) aiResponse.get("regex"))
                        .extractedData(extractedData)
                        .build());

            } catch (Exception e) {
                log.error("Errore durante l'elaborazione di {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return results;
    }

    // --- AI PIPELINE HELPERS ---

    /**
     * Chiama Python passando il file e le regex del template salvato
     */
    private Map<String, Object> callPythonExtractor(MultipartFile file) {
        return callPythonEndpoint(file, "/extract", null);
    }

    private Map<String, Object> callPythonWithTemplate(MultipartFile file, Map<String, String> patterns) {
        try {
            String patternsJson = objectMapper.writeValueAsString(patterns);
            return callPythonEndpoint(file, "/extract-by-template", patternsJson);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> callPythonEndpoint(MultipartFile file, String endpoint, String rules) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename(); }
            });

            if (rules != null) {
                body.add("template_rules", rules);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(extractorUrl + endpoint, requestEntity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Errore chiamata Python {}: {}", endpoint, e.getMessage());
            return null;
        }
    }

    private String extractMarkdown(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Pulizia del nome file: rimuoviamo apostrofi e caratteri non standard
            String sanitizedFilename = file.getOriginalFilename()
                    .replaceAll("['\\s]", "_") // Sostituisce apostrofi e spazi con underscore
                    .replaceAll("[^a-zA-Z0-9._-]", ""); // Rimuove tutto il resto tranne punti e trattini

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return sanitizedFilename;
                }
            };

            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    extractorUrl + "/extract",
                    requestEntity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("markdown")) {
                return (String) response.getBody().get("markdown");
            }
            return null;
        } catch (Exception e) {
            // Logga l'eccezione completa per vedere la causa reale (es. Timeout o Connection Reset)
            log.error("Estrazione Fallita per {}: {}", file.getOriginalFilename(), e.toString());
            return null;
        }
    }

    @Async
    public void sendToLightRagAsync(String title, String content) {
        try {
            Map<String, String> payload = Map.of("title", title, "content", content);
            restTemplate.postForEntity(lightragUrl + "/insert", payload, String.class);
            log.info("LightRAG: {} indicizzato con successo", title);
        } catch (Exception e) {
            log.error("LightRAG Error: {}", e.getMessage());
        }
    }

    // --- UTILS ---

    private String calculateSHA256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(file.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private int parseYear(Map<String, Object> data, String filename) {
        try {
            // Navigazione sicura della mappa annidata
            Map<String, Object> periodo = (Map<String, Object>) data.get("periodo");
            if (periodo != null && periodo.get("anno") != null) {
                return Integer.parseInt(periodo.get("anno").toString().replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            log.warn("Anno non trovato nei dati AI per {}, uso fallback", filename);
        }
        return LocalDate.now().getYear();
    }

    private int parseMonth(Map<String, Object> data) {
        try {
            Map<String, Object> periodo = (Map<String, Object>) data.get("periodo");
            if (periodo != null && periodo.get("mese") != null) {
                String m = periodo.get("mese").toString().toLowerCase();
                if (m.contains("gen")) return 1; if (m.contains("feb")) return 2;
                if (m.contains("mar")) return 3; if (m.contains("apr")) return 4;
                if (m.contains("mag")) return 5; if (m.contains("giu")) return 6;
                if (m.contains("lug")) return 7; if (m.contains("ago")) return 8;
                if (m.contains("set")) return 9; if (m.contains("ott")) return 10;
                if (m.contains("nov")) return 11; if (m.contains("dic")) return 12;

                String monthNum = m.replaceAll("[^0-9]", "");
                if (!monthNum.isEmpty()) return Integer.parseInt(monthNum);
            }
        } catch (Exception e) {
            log.warn("Impossibile parsare il mese dai dati AI");
        }
        return LocalDate.now().getMonthValue(); // Fallback
    }

    // --- REPOSITORY & STORAGE HELPERS ---

    private String uploadToMinio(MultipartFile file, String userId) {
        // Creiamo il nome file con UUID per evitare collisioni
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Costruiamo il path completo: "ID_UTENTE/UUID_NOMEFILE.ext"
        String fullPath = userId + "/" + filename;

        try {
            // Usiamo uploadPayslip passando il fullPath come 'filename' (che MinIO userà come Object Key)
            minIOService.uploadPayslip(file.getInputStream(), fullPath, file.getContentType());

            // Restituiamo il path completo perché è quello che dovrai salvare nel DB
            // per poter recuperare il file in futuro
            return fullPath;
        } catch (IOException e) {
            log.error("Errore durante l'upload su MinIO per l'utente {}: {}", userId, e.getMessage());
            throw new RuntimeException("Errore upload MinIO");
        }
    }

    private YearFolder getOrCreateYearFolder(String userId, int year) {
        return yearFolderRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> yearFolderRepository.save(YearFolder.builder()
                        .userId(userId).year(year).color(getRandomColor()).months(new ArrayList<>()).build()));
    }

    private MonthFolder getOrCreateMonthFolder(YearFolder yearFolder, int month) {
        return yearFolder.getMonths().stream()
                .filter(m -> m.getMonth() == month)
                .findFirst()
                .orElseGet(() -> {
                    MonthFolder nf = MonthFolder.builder()
                            .id(UUID.randomUUID().toString())
                            .month(month)
                            .name(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ITALIAN))
                            .files(new ArrayList<>())
                            .build();
                    yearFolder.getMonths().add(nf);
                    yearFolder.getMonths().sort(Comparator.comparingInt(MonthFolder::getMonth).reversed());
                    return nf;
                });
    }

    private FileItem buildFileItem(MultipartFile file, String url) {
        return FileItem.builder()
                .id(UUID.randomUUID().toString())
                .name(file.getOriginalFilename())
                .url(url)
                .type("pdf")
                .size(file.getSize())
                .uploadDate(Instant.now())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String getRandomColor() {
        String[] colors = {"#FF5733", "#33FF57", "#3357FF", "#F1C40F", "#9B59B6", "#E67E22"};
        return colors[new Random().nextInt(colors.length)];
    }

    public Payslip confirmAndSave(PayslipResponseDto dto) {
        User user = getCurrentUser();

        // 1. Salvataggio o Recupero Template
        PayrollTemplate template = payrollTemplateRepository.findBySignatureAndUserId(dto.getSignature(), user.getId())
                .orElseGet(() -> payrollTemplateRepository.save(PayrollTemplate.builder()
                        .name("Template " + dto.getAzienda())
                        .signature(dto.getSignature())
                        .userId(user.getId())
                        .regexPatterns(dto.getRegex())
                        .build()));

        // 2. Salvataggio Payslip definitiva
        Payslip payslip = Payslip.builder()
                .templateId(template.getId())
                .fileName("Estratto da " + dto.getAzienda())
                .extractedData(dto.getExtractedData())
                .build();

        return payslipRepository.save(payslip);
    }

    public List<PayrollTemplate> getUserTemplates(String userId) {
        log.info("Recupero template per l'utente ID: {}", userId);
        return payrollTemplateRepository.findByUserId(userId);
    }

    /**
     * Recupera tutte le Payslip dell'utente convertendole nel formato richiesto dal DTO
     */
    public List<Payslip> getAllUserPayslips() {
        User user = getCurrentUser();
        log.info("Recupero lista buste paga per utente: {}", user.getEmail());

        return payslipRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}