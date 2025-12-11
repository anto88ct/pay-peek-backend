package com.paypeek.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "anomalies")
public class Anomaly {
    @Id
    private String id;
    private String userId;
    private String type;
    private String severity;
    private String message;
    private Map<String, Object> data;
    private String status;
    private Date createdAt;
}
