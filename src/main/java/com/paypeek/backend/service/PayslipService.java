package com.paypeek.backend.service;

import com.paypeek.backend.dto.ErrorResponseDto;
import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.MassUploadResponseDto;
import com.paypeek.backend.dto.mapper.YearFolderMapper;
import com.paypeek.backend.dto.PayslipResponseDto;
import com.paypeek.backend.model.*;
import com.paypeek.backend.repository.PayrollTemplateRepository;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.repository.YearFolderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.MessageDigest;
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
    private final AIService aiService;
    private final LightRagService lightRagService;

    public PayslipService(MinIOService minIOService,
                          YearFolderRepository yearFolderRepository,
                          UserRepository userRepository,
                          YearFolderMapper yearFolderMapper,
                          PayrollTemplateRepository payrollTemplateRepository,
                          PayslipRepository payslipRepository,
                          AIService aiService,
                          LightRagService lightRagService) {
        this.minIOService = minIOService;
        this.yearFolderRepository = yearFolderRepository;
        this.userRepository = userRepository;
        this.yearFolderMapper = yearFolderMapper;
        this.payrollTemplateRepository = payrollTemplateRepository;
        this.payslipRepository = payslipRepository;
        this.aiService = aiService;
        this.lightRagService = lightRagService;
    }


    /**
     * CORE LOGIC: Crea un nuovo Template e la prima Payslip dinamica
     */
    public Payslip buildPayslipTemplate(MultipartFile file) {
        User user = getCurrentUser(); // Recupera l'utente
        log.info("Inizio buildPayslipTemplate per utente: {} - file: {}", user.getEmail(), file.getOriginalFilename());

        Map<String, Object> response = aiService.callPythonExtractor(file);

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
        String markdown = aiService.extractMarkdown(file);

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
            lightRagService.sendToLightRagAsync(file.getOriginalFilename(), markdown);
        }

        return yearFolderMapper.toDto(fileItem);
    }

    /**
     * MASS UPLOAD: Rileva automaticamente se usare Template (Regex) o AI Vision pura
     */
    public List<PayslipResponseDto> massUpload(List<MultipartFile> files) {
        User user = getCurrentUser();
        List<PayslipResponseDto> results = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();

            // Inizializziamo il DTO per il file corrente con una lista errori vuota
            PayslipResponseDto response = PayslipResponseDto.builder()
                    .extractionErrors(new ArrayList<>())
                    .build();

            try {
                // 1. Calcolo Signature locale
                String signature = calculateSHA256(file);
                response.setSignature(signature);

                // 2. Controllo esistenza Template
                Optional<PayrollTemplate> templateOpt = payrollTemplateRepository.findBySignatureAndUserId(signature, user.getId());

                Map<String, Object> aiResponse;
                if (templateOpt.isPresent()) {
                    log.info("Documento noto. Uso estrazione guidata per: {}", fileName);
                    aiResponse = aiService.callPythonWithTemplate(file, templateOpt.get().getRegexPatterns());
                } else {
                    log.info("Documento nuovo. Uso AI Vision generica per: {}", fileName);
                    aiResponse = aiService.callPythonExtractor(file);
                }

                // Estrazione dati
                Map<String, Object> extractedData = (Map<String, Object>) aiResponse.get("extractedData");

                // --- GESTIONE ERRORE ESTRAZIONE (AI fallita o dati nulli) ---
                if (extractedData == null) {
                    log.error("L'estrattore non ha restituito dati validi per {}", fileName);

                    response.getExtractionErrors().add(ErrorResponseDto.builder()
                            .type("EXTRACTION_ERROR")
                            .statusCode(422)
                            .message("L'intelligenza artificiale non ha rilevato dati validi nel file: " + fileName)
                            .path(fileName)
                            .timestamp(Instant.now())
                            .build());

                    results.add(response);
                    continue; // Passa al file successivo senza salvare nulla
                }

                // 3. Determinazione Anno e Mese
                int year = parseYear(extractedData, fileName);
                int month = parseMonth(extractedData);

                // 4. Upload Fisico su MinIO
                String minioUrl = uploadToMinio(file, user.getId());

                // 5. Salvataggio record Payslip su MongoDB
                Payslip payslip = Payslip.builder()
                        .userId(user.getId())
                        .templateId(templateOpt.map(PayrollTemplate::getId).orElse("AUTO_GENERATED"))
                        .fileName(fileName)
                        .extractedData(extractedData)
                        .build();
                payslipRepository.save(payslip);

                // 6. Organizzazione Folder MongoDB (UI "stile cartelle")
                YearFolder yearFolder = getOrCreateYearFolder(user.getId(), year);
                MonthFolder monthFolder = getOrCreateMonthFolder(yearFolder, month);

                FileItem fileItem = FileItem.builder()
                        .id(UUID.randomUUID().toString())
                        .name(fileName)
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

                // 7. Popolamento DTO di risposta (Successo)
                response.setAzienda((String) aiResponse.getOrDefault("azienda", "Sconosciuta"));
                response.setRegex((Map<String, String>) aiResponse.get("regex"));
                response.setExtractedData(extractedData);

            } catch (Exception e) {
                log.error("Errore critico durante l'elaborazione di {}: {}", fileName, e.getMessage());

                // Aggiungiamo l'errore tecnico alla lista del DTO
                response.getExtractionErrors().add(ErrorResponseDto.builder()
                        .type("PROCESS_ERROR")
                        .statusCode(500)
                        .message("Errore durante l'elaborazione di " + fileName + ": " + e.getMessage())
                        .path(fileName)
                        .timestamp(Instant.now())
                        .build());
            }

            results.add(response);
        }
        return results;
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