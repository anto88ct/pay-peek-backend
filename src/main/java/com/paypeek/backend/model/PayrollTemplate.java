package com.paypeek.backend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "payroll_templates")
@Data
@Builder
public class PayrollTemplate extends BaseEntity{

    private String name;
    private String signature;
    private String userId;

    // Mappa delle Regex generate dall'IA
    // Chiave: "netto", Valore: "Regex per il netto"
    private Map<String, String> regexPatterns;
}