package com.paypeek.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "notification_logs")
@CompoundIndex(name = "user_month_year_idx", def = "{'userId': 1, 'refMonth': 1, 'refYear': 1}", unique = true) // Indice per evitare invii duplicati per lo stesso mese/utente
public class MonthlyNotificationLog extends BaseEntity {

    private String userId;
    private Integer refMonth; // 12
    private Integer refYear;  // 2025
    private List<String> presentPayslips;
    private List<String> missingPayslips;
    private LocalDateTime sentAt;
    private String deliveryStatus; // SUCCESS / FAILED
    private String errorDetails;
}