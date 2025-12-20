package com.paypeek.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class BiometricRegisterRequest {
    @NotBlank
    private String email;

    @NotNull
    private PublicKeyCredentialJson credential;
}
