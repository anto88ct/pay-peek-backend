package com.paypeek.backend.service;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.mapper.YearFolderMapper;
import com.paypeek.backend.model.FileItem;
import com.paypeek.backend.model.MonthFolder;
import com.paypeek.backend.model.User;
import com.paypeek.backend.model.YearFolder;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.repository.YearFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PayslipService {

    private final MinIOService minIOService;
    private final YearFolderRepository yearFolderRepository;
    private final UserRepository userRepository;
    private final YearFolderMapper yearFolderMapper;
    private final RestTemplate restTemplate;
    private final PayslipService self;

    @Value("${app.extractor.url:http://paypeek-extractor:8000}")
    private String extractorUrl;

    @Value("${app.lightrag.url:http://paypeek-lightrag:8020}")
    private String lightragUrl;

    public PayslipService(MinIOService minIOService,
                          YearFolderRepository yearFolderRepository,
                          UserRepository userRepository,
                          YearFolderMapper yearFolderMapper,
                          @Lazy PayslipService self) {
        this.minIOService = minIOService;
        this.yearFolderRepository = yearFolderRepository;
        this.userRepository = userRepository;
        this.yearFolderMapper = yearFolderMapper;
        this.restTemplate = new RestTemplate();
        this.self = self;
    }

    /**
     * UPLOAD SINGOLO: Carica in una cartella specifica (ID mese fornito dal FE)
     */
    public FileItemDto uploadFile(String monthFolderId, MultipartFile file) {
        User user = getCurrentUser();

        // 1. Estrazione testo per LightRAG
        String markdown = extractMarkdown(file);

        // 2. Upload su MinIO
        String minioFilename = uploadToMinio(file);

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

    /**
     * MASS UPLOAD: Rileva automaticamente Anno/Mese dal contenuto del PDF
     */
    public List<FileItemDto> massUpload(List<MultipartFile> files) {
        User user = getCurrentUser();
        List<FileItemDto> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 1. Estrazione Markdown (Sincrona per poter analizzare il periodo)
                String markdown = extractMarkdown(file);

                // 2. Analisi AI del periodo (Anno e Mese)
                Map<String, Integer> period = detectPeriod(markdown, file.getOriginalFilename());
                int year = period.get("year");
                int month = period.get("month");

                // 3. Upload fisico su MinIO
                String minioFilename = uploadToMinio(file);

                // 4. Organizzazione cartelle dinamica
                YearFolder yearFolder = getOrCreateYearFolder(user.getId(), year);
                MonthFolder monthFolder = getOrCreateMonthFolder(yearFolder, month);

                // 5. Salvataggio metadati
                FileItem fileItem = buildFileItem(file, minioFilename);
                monthFolder.getFiles().add(fileItem);
                yearFolderRepository.save(yearFolder);

                // 6. Indicizzazione AI asincrona
                if (markdown != null) {
                    self.sendToLightRagAsync(file.getOriginalFilename(), markdown);
                }

                results.add(yearFolderMapper.toDto(fileItem));
                log.info("File {} elaborato correttamente per il periodo {}/{}", file.getOriginalFilename(), month, year);

            } catch (Exception e) {
                log.error("Errore durante l'elaborazione di {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return results;
    }

    // --- AI PIPELINE HELPERS ---

    private String extractMarkdown(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename(); }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(extractorUrl + "/extract", requestEntity, Map.class);

            return (String) Objects.requireNonNull(response.getBody()).get("markdown");
        } catch (Exception e) {
            log.error("Estrazione Fallita: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Analizza il testo per estrarre Anno e Mese.
     * Implementa una logica basata su Regex che cerca pattern tipici delle buste paga italiane.
     */
    private Map<String, Integer> detectPeriod(String markdown, String filename) {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        if (markdown == null) return Map.of("year", year, "month", month);

        // Regex per Anno (cerca 2020-2029)
        Pattern yearPattern = Pattern.compile("202[0-9]");
        Matcher yearMatcher = yearPattern.matcher(markdown);
        if (yearMatcher.find()) {
            year = Integer.parseInt(yearMatcher.group());
        }

        // Regex per Mese (nomi italiani o numeri 01-12)
        String[] monthsIt = {"gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
                "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"};

        String lowerMarkdown = markdown.toLowerCase();
        for (int i = 0; i < monthsIt.length; i++) {
            if (lowerMarkdown.contains(monthsIt[i])) {
                month = i + 1;
                break;
            }
        }

        return Map.of("year", year, "month", month);
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

    // --- REPOSITORY & STORAGE HELPERS ---

    private String uploadToMinio(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minIOService.uploadPayslip(file.getInputStream(), filename, file.getContentType());
            return filename;
        } catch (IOException e) {
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
}