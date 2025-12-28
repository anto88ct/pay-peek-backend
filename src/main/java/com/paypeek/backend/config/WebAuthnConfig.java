package com.paypeek.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.CborConverter;
import com.webauthn4j.converter.util.JsonConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfig {

    @Bean
    public ObjectConverter objectConverter() {
        // Questo costruttore inizializza internamente sia il JSON mapper
        // che il CBOR mapper (richiede la dipendenza jackson-dataformat-cbor)
        return new ObjectConverter();
    }

    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        // Usa il metodo factory standard che imposta tutto correttamente
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }
}
