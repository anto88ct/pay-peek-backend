package com.paypeek.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem {
    private String id;
    private String name;
    private String url;
    private String type;
    private Long size;
    private Instant uploadDate;

    // Campi aggiuntivi persistenti
    private int anno;
    private int mese;
    private Map<String, Object> dati;
}
