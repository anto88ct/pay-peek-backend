package com.paypeek.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class LightRagService {

    @Value("${app.lightrag.url:http://paypeek-lightrag:8020}")
    private String lightragUrl;

    private final RestTemplate restTemplate;


    public LightRagService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(300))
                .build();
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
}
