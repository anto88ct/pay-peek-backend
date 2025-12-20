package com.paypeek.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicKeyCredentialJson {

    private String id;

    @JsonProperty("rawId")
    private String rawId;

    private String type;

    @JsonProperty("response")
    private AuthenticatorResponse response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthenticatorResponse {
        // Shared fields
        private String clientDataJSON;

        // Registration specific
        private String attestationObject;

        // Login specific (Assertion)
        private String authenticatorData;
        private String signature;
        private String userHandle;
    }
}
