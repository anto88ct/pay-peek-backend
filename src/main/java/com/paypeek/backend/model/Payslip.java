package com.paypeek.backend.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document(collection = "payslips")
@Data
@Builder
public class Payslip extends BaseEntity {

    private String userId;
    private String templateId; // Collegamento al PayrollTemplate creato
    private String fileName;

    // Qui salviamo i dati dinamici tornati dallo script Python
    // Esempio: { "netto": 1647.0, "azienda": "SEEDMA S.R.L.", "bonus": 100.0 }
    private Map<String, Object> extractedData;
}