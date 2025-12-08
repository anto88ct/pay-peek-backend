package main.java.com.paypeek.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payslips")
public class Payslip {
    @Id
    private String id;
    private String userId;
    private Date uploadDate;
    private String fileUrl; // path MinIO
    private String processingStatus; // PENDING, COMPLETED, ERROR
    private String processingError;
    private String extractedText;
    private Double confidence;
}
