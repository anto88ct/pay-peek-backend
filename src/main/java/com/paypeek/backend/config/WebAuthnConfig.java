package com.paypeek.backend.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfig {

    /**
     * ✅ ObjectConverter bean
     */
    @Bean
    public ObjectConverter objectConverter() {
        return new ObjectConverter();
    }

    /**
     * ✅ WebAuthnManager 0.20.0 - usa createNonStrictWebAuthnManager()
     */
    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }
}
