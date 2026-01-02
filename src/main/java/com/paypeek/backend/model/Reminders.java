package com.paypeek.backend.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "reminders")
@CompoundIndex(name = "user_month_year_idx", def = "{'userId': 1, 'refMonth': 1, 'refYear': 1}", unique = true) // Indice per evitare invii duplicati per lo stesso mese/utente
public class Reminders extends BaseEntity {

    private String userId;
    private Integer refMonth;                   // Mese di riferimento (es: 12)
    private Integer refYear;                    // Anno di riferimento (es: 2025)
    private List<String> presentPayslips;       // Storico documenti trovati
    private List<String> missingPayslips;       // Storico documenti mancanti
    private String frontendMessage;             // Messaggio specifico per l'ultimo mese
    private LocalDateTime sentAt;
    private String deliveryStatus;              // SUCCESS / FAILED
    private String errorDetails;
}