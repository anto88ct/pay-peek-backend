package main.java.com.paypeek.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "dati_busta_paga")
public class DatiBustaPaga {
    @Id
    private String id;
    private String userId;
    private String payslipId;
    private Integer year;
    private Integer month;
    private Map<String, Object> indicators;
    private String rawData;
    private Double extractionQuality;
}
