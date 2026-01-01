package com.paypeek.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class AIService {

    //    @Value("${app.extractor.url:http://paypeek-extractor:8000}") //PROD
    @Value("${app.extractor.url:http://localhost:8000}") //DEV
    private String extractorUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AIService(ObjectMapper objectMapper,
                     RestTemplateBuilder restTemplateBuilder) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(300))
                .build();
    }


    /**
     * Chiama Python passando il file e le regex del template salvato
     */
    public Map<String, Object> callPythonExtractor(MultipartFile file) {
        return callPythonEndpoint(file, "/extract", null);
    }

    public Map<String, Object> callPythonWithTemplate(MultipartFile file, Map<String, String> patterns) {
        try {
            String patternsJson = objectMapper.writeValueAsString(patterns);
            return callPythonEndpoint(file, "/extract-by-template", patternsJson);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> callPythonEndpoint(MultipartFile file, String endpoint, String rules) {
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

    public String extractMarkdown(MultipartFile file) {
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
}
