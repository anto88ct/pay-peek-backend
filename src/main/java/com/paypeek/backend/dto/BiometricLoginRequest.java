package com.paypeek.backend.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class BiometricLoginRequest {
    private String email;

    @JsonProperty("assertion")
    private PublicKeyCredentialJson assertion;
}
