package com.paypeek.backend.model;

import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_credentials")
public class UserCredential extends BaseEntity {

    private String userId;
    private String credentialId;
    private byte[] publicKey;
    private long signCount;
    private List<String> transports;
    private Instant lastUsed;
}
