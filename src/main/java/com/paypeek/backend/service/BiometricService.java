package com.paypeek.backend.service;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.BiometricLoginRequest;
import com.paypeek.backend.dto.BiometricRegisterRequest;
import com.paypeek.backend.dto.UserDto;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.paypeek.backend.dto.mapper.UserMapper;
import com.paypeek.backend.exception.BiometricAuthException;
import com.paypeek.backend.exception.BiometricNotAvailableException;
import com.paypeek.backend.exception.UserNotFoundException;
import com.paypeek.backend.model.User;
import com.paypeek.backend.model.UserCredential;
import com.paypeek.backend.repository.UserCredentialRepository;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.security.JwtService;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.AuthenticatorDataConverter;
import com.webauthn4j.converter.CollectedClientDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.CollectedClientData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BiometricService {

    private final WebAuthnManager webAuthnManager;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ObjectConverter objectConverter;

    // In-memory challenge storage (production: use Redis)
    private final Map<String, Challenge> challengeStore = new ConcurrentHashMap<>();

    private static final String RP_ID = "localhost";
    private static final String RP_NAME = "PayPeek";
    private static final Origin ORIGIN = new Origin("http://localhost:4200");

    @PostConstruct
    public void init() {
        try {
            String version = com.webauthn4j.WebAuthnManager.class.getPackage().getImplementationVersion();
            log.info("🚀 [STARTUP] WebAuthn4j Version: {}", version);
        } catch (Exception e) {
            log.warn("⚠️ [STARTUP] Impossibile determinare la versione di WebAuthn4j");
        }
    }

    /**
     * Generate challenge for registration
     */
    public PublicKeyCredentialCreationOptions generateRegisterChallenge(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + email));

        Challenge challenge = new DefaultChallenge();
        challengeStore.put(email, challenge);

        // Exclude existing credentials
        List<UserCredential> existingCredentials = userCredentialRepository.findByUserId(user.getId());
        List<PublicKeyCredentialDescriptor> excludeCredentials = existingCredentials.stream()
                .map(this::toPublicKeyCredentialDescriptor)
                .collect(Collectors.toList());

        return new PublicKeyCredentialCreationOptions(
                new PublicKeyCredentialRpEntity(RP_ID, RP_NAME),
                new PublicKeyCredentialUserEntity(
                        user.getId().getBytes(),
                        user.getEmail(),
                        user.getFirstName() + " " + user.getLastName()),
                challenge,
                List.of(new PublicKeyCredentialParameters(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        COSEAlgorithmIdentifier.ES256)),
                null,
                excludeCredentials,
                new AuthenticatorSelectionCriteria(
                        AuthenticatorAttachment.PLATFORM,
                        true,
                        UserVerificationRequirement.PREFERRED),
                AttestationConveyancePreference.NONE,
                null);
    }

    private byte[] decodeBase64(String input) {
        if (input == null || input.isEmpty()) return new byte[0];

        // 1. Pulizia minima (solo spazi e ritorni a capo)
        String cleaned = input.trim().replaceAll("\\s", "");

        // 2. Normalizzazione da Standard Base64 a URL-Safe (se necessario)
        cleaned = cleaned.replace('+', '-').replace('/', '_');

        // 3. Rimuovi il padding '=' per sicurezza prima della decodifica URL-safe
        cleaned = cleaned.replace("=", "");

        try {
            return Base64.getUrlDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            log.error("❌ Errore decodifica Base64: {}", e.getMessage());
            throw new BiometricAuthException("Formato Base64 non valido");
        }
    }

    /**
     * Register biometric credential
     */
    public AuthResponse registerBiometric(BiometricRegisterRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        Challenge challenge = challengeStore.remove(email);
        if (challenge == null) throw new BiometricAuthException("Challenge scaduta");

        try {

            // 1. Decodifica i byte array dai dati Base64
            byte[] clientDataBytes = decodeBase64(request.getCredential().getResponse().getClientDataJSON());
            byte[] attestationObjectBytes = decodeBase64(request.getCredential().getResponse().getAttestationObject());

            // 2. PARSING MANUALE (Bypass del bug interno)
            // Usiamo i converter della libreria ma li invochiamo noi direttamente
            CollectedClientDataConverter clientDataConverter = new CollectedClientDataConverter(objectConverter);
            AttestationObjectConverter attestationConverter = new AttestationObjectConverter(objectConverter);

            // Se il bug è nel JSON parser, qui possiamo gestirlo
            CollectedClientData collectedClientData = clientDataConverter.convert(clientDataBytes);
            AttestationObject attestationObject = attestationConverter.convert(attestationObjectBytes);

            // 3. COSTRUZIONE MANUALE DI REGISTRATION DATA
            // Questo oggetto contiene già i dati parsati, quindi il 'verify' non dovrà più parsare nulla
            RegistrationData registrationData = new RegistrationData(
                    attestationObject,
                    attestationObjectBytes,
                    collectedClientData,
                    clientDataBytes,
                    null, // clientExtensionResults
                    null  // transports
            );

            // 4. VALIDAZIONE LOGICA E CRITTOGRAFICA
            ServerProperty serverProperty = new ServerProperty(ORIGIN, RP_ID, challenge, null);
            RegistrationParameters registrationParameters = new RegistrationParameters(
                    serverProperty,
                    null,
                    true // selfAttestationAllowed
            );

            // Usiamo il metodo verify(RegistrationData, ...) invece di validate(RegistrationRequest, ...)
            // Questo metodo NON esegue il parsing ma solo il controllo di firme, challenge e origin
            webAuthnManager.verify(registrationData, registrationParameters);

            // 5. SALVATAGGIO
            byte[] credentialId = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCredentialId();
            byte[] publicKey = objectConverter.getCborConverter().writeValueAsBytes(
                    registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCOSEKey()
            );

            UserCredential credential = UserCredential.builder()
                    .userId(user.getId())
                    .credentialId(Base64.getUrlEncoder().withoutPadding().encodeToString(credentialId))
                    .publicKey(publicKey)
                    .signCount(registrationData.getAttestationObject().getAuthenticatorData().getSignCount())
                    .build();

            userCredentialRepository.save(credential);
            user.setBiometricEnabled(true);
            userRepository.save(user);

            return new AuthResponse(jwtService.generateToken(buildUserDetails(user)), userMapper.toDto(user));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Errore: {}", e.getMessage(), e);
            throw new BiometricAuthException("Validazione fallita: " + e.getMessage());
        }
    }

// ==================== METODI HELPER DI SUPPORTO ====================

    /**
     * Estrae la stringa Base64 gestendo sia il formato stringa semplice
     * che l'eventuale incapsulamento in un oggetto JSON {"value": "..."}
     */
    private String extractBase64(Object input) {
        if (input == null) return "";
        if (input instanceof String) return (String) input;
        if (input instanceof Map) {
            return (String) ((Map<?, ?>) input).get("value");
        }
        return input.toString();
    }

    /**
     * Rimuove il Byte Order Mark (BOM) UTF-8 (EF BB BF) se presente all'inizio del byte array.
     * Jackson fallisce miseramente se trova questi byte all'inizio di un file JSON.
     */
    private byte[] stripBom(byte[] data) {
        if (data.length >= 3 &&
                (data[0] & 0xFF) == 0xEF &&
                (data[1] & 0xFF) == 0xBB &&
                (data[2] & 0xFF) == 0xBF) {
            log.warn("⚠️ [BIOMETRIC] Rilevato e rimosso UTF-8 BOM dai dati ricevuti");
            byte[] stripped = new byte[data.length - 3];
            System.arraycopy(data, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return data;
    }

    /**
     * Generate challenge for login
     */
    public PublicKeyCredentialRequestOptions generateLoginChallenge(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + email));

        List<UserCredential> credentials = userCredentialRepository.findByUserId(user.getId());
        if (credentials.isEmpty()) {
            throw new BiometricNotAvailableException("Nessuna credenziale biometrica registrata per questo utente");
        }

        Challenge challenge = new DefaultChallenge();
        challengeStore.put(email, challenge);

        List<PublicKeyCredentialDescriptor> allowCredentials = credentials.stream()
                .map(this::toPublicKeyCredentialDescriptor)
                .collect(Collectors.toList());

        return new PublicKeyCredentialRequestOptions(
                challenge,
                60000L,
                RP_ID,
                allowCredentials,
                UserVerificationRequirement.PREFERRED,
                null);
    }

    /**
     * Login con biometria (Assertion)
     * Versione aggiornata con Manual Parsing Bypass
     */
    public AuthResponse loginBiometric(BiometricLoginRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + email));

        // 1. Recupero Challenge
        Challenge challenge = challengeStore.remove(email);
        if (challenge == null) {
            throw new BiometricAuthException("Challenge scaduta o non trovata. Riprova il login.");
        }

        try {
            String credentialIdStr = request.getAssertion().getId();

            // 2. Recupero credenziale e chiave pubblica dal DB
            UserCredential credential = userCredentialRepository.findByCredentialId(credentialIdStr)
                    .orElseThrow(() -> new BiometricAuthException("Credenziale biometrica non trovata nel database"));

            if (!credential.getUserId().equals(user.getId())) {
                throw new BiometricAuthException("Questa credenziale non appartiene all'utente specificato");
            }

            // 3. Decodifica Base64URL di tutti i componenti dell'assertion
            byte[] credentialId = decodeBase64(credentialIdStr);
            byte[] clientDataBytes = decodeBase64(request.getAssertion().getResponse().getClientDataJSON());
            byte[] authDataBytes = decodeBase64(request.getAssertion().getResponse().getAuthenticatorData());
            byte[] signature = decodeBase64(request.getAssertion().getResponse().getSignature());

            String rawUserHandle = request.getAssertion().getResponse().getUserHandle();
            byte[] userHandle = (rawUserHandle != null) ? decodeBase64(rawUserHandle) : null;

            // 4. PARSING MANUALE (Bypass del bug Jackson interno a WebAuthn4j)
            CollectedClientDataConverter clientDataConverter = new CollectedClientDataConverter(objectConverter);
            AuthenticatorDataConverter authDataConverter = new AuthenticatorDataConverter(objectConverter);

            CollectedClientData collectedClientData = clientDataConverter.convert(clientDataBytes);
            AuthenticatorData authenticatorData = authDataConverter.convert(authDataBytes);

            // 5. Ricostruzione dell'Authenticator con Chiave Pubblica DB
            com.webauthn4j.data.attestation.authenticator.COSEKey publicKey =
                    objectConverter.getCborConverter().readValue(credential.getPublicKey(),
                            com.webauthn4j.data.attestation.authenticator.COSEKey.class);

            Authenticator authenticator = new AuthenticatorImpl(
                    new AttestedCredentialData(
                            AAGUID.ZERO,
                            decodeBase64(credential.getCredentialId()),
                            publicKey
                    ),
                    null,
                    credential.getSignCount()
            );

            // 6. Costruzione dell'oggetto AuthenticationData per la verifica
            // Nota l'ordine esatto dei parametri richiesti dal costruttore della 0.28.x
            AuthenticationData authenticationData = new AuthenticationData(
                    credentialId,       // 1. @Nullable byte[] credentialId
                    userHandle,         // 2. @Nullable byte[] userHandle
                    authenticatorData,  // 3. @Nullable AuthenticatorData authenticatorData (oggetto parsato)
                    authDataBytes,      // 4. @Nullable byte[] authenticatorDataBytes (byte grezzi)
                    collectedClientData,// 5. @Nullable CollectedClientData collectedClientData (oggetto parsato)
                    clientDataBytes,    // 6. @Nullable byte[] collectedClientDataBytes (byte grezzi)
                    null,               // 7. @Nullable AuthenticationExtensionsClientOutputs clientExtensions
                    signature           // 8. @Nullable byte[] signature
            );

            // 7. Validazione Crittografica (Firma, Challenge, Origin, SignCount)
            ServerProperty serverProperty = new ServerProperty(ORIGIN, RP_ID, challenge, null);
            AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                    serverProperty,
                    authenticator,
                    false // userVerificationRequired (setta a true se vuoi forzare FaceID/PIN)
            );

            // Usiamo verify() invece di validate() per coerenza con le nuove versioni
            webAuthnManager.verify(authenticationData, authenticationParameters);

            // 8. Aggiornamento Sign Count (Protezione contro il replay attack)
            credential.setSignCount(authenticationData.getAuthenticatorData().getSignCount());
            userCredentialRepository.save(credential);

            log.info("✅ [BIOMETRIC] Login completato con successo per l'utente: {}", email);

            // Generazione Token JWT e risposta
            return new AuthResponse(jwtService.generateToken(buildUserDetails(user)), userMapper.toDto(user));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Errore critico durante il login di {}: {}", email, e.getMessage());
            throw new BiometricAuthException("Autenticazione biometrica fallita: " + e.getMessage());
        }
    }

    /**
     * Check if user has biometric credentials registered
     */
    public boolean isBiometricRegistered(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        return !userCredentialRepository.findByUserId(user.getId()).isEmpty();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * ✅ HELPER: Crea UserDetails da User model di Pay Peek
     */
    private UserDetails buildUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // ✅ FIXED: Usa fully qualified name direttamente
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!user.isEnabled())
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }

    private PublicKeyCredentialDescriptor toPublicKeyCredentialDescriptor(UserCredential c) {
        Set<AuthenticatorTransport> transports = null;
        if (c.getTransports() != null && !c.getTransports().isEmpty()) {
            transports = c.getTransports().stream()
                    .map(AuthenticatorTransport::create)
                    .collect(Collectors.toSet());
        }

        return new PublicKeyCredentialDescriptor(
                PublicKeyCredentialType.PUBLIC_KEY,
                Base64.getUrlDecoder().decode(c.getCredentialId()),
                transports);
    }
}
