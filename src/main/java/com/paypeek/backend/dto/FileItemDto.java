package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemDto {
    private String id;
    private String name;
    private String url;
    private String type;
    private Long size;
    private Instant uploadDate;

    // Nuovi campi per il FE
    private int anno;
    private int mese;
    private Map<String, Object> dati;
}