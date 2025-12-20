package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
public class BiometricCheckResponse {

    @JsonProperty("registered")
    private boolean registered;
}
