package com.paypeek.backend.service;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.BiometricLoginRequest;
import com.paypeek.backend.dto.BiometricRegisterRequest;
import com.paypeek.backend.dto.UserDto;
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
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Register biometric credential
     */
    public AuthResponse registerBiometric(BiometricRegisterRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + email));

        Challenge challenge = challengeStore.remove(email);
        if (challenge == null) {
            throw new BiometricAuthException("Challenge scaduta o non trovata");
        }

        try {
            // Decode WebAuthn response
            byte[] clientDataJSON = Base64.getUrlDecoder()
                    .decode(request.getCredential().getResponse().getClientDataJSON());
            byte[] attestationObject = Base64.getUrlDecoder()
                    .decode(request.getCredential().getResponse().getAttestationObject());

            ServerProperty serverProperty = new ServerProperty(ORIGIN, RP_ID, challenge, null);
            RegistrationRequest registrationRequest = new RegistrationRequest(clientDataJSON, attestationObject);

            RegistrationParameters registrationParameters = new RegistrationParameters(
                    serverProperty,
                    null,
                    false
            );

            // Validate registration
            RegistrationData registrationData = webAuthnManager.validate(registrationRequest, registrationParameters);

            // Extract and save credential
            byte[] credentialId = registrationData.getAttestationObject()
                    .getAuthenticatorData()
                    .getAttestedCredentialData()
                    .getCredentialId();
            com.webauthn4j.data.attestation.authenticator.COSEKey coseKey = registrationData.getAttestationObject()
                    .getAuthenticatorData()
                    .getAttestedCredentialData()
                    .getCOSEKey();
            long signCount = registrationData.getAttestationObject()
                    .getAuthenticatorData()
                    .getSignCount();

            UserCredential credential = UserCredential.builder()
                    .userId(user.getId())
                    .credentialId(Base64.getUrlEncoder().withoutPadding().encodeToString(credentialId))
                    .publicKey(objectConverter.getCborConverter().writeValueAsBytes(coseKey))
                    .signCount(signCount)
                    .transports(Collections.emptyList())
                    .build();

            userCredentialRepository.save(credential);

            // Enable biometric preference
            user.setBiometricEnabled(true);
            userRepository.save(user);

            log.info("Biometric registered successfully for email: {}", email);

            // ✅ FIXED: Usa fully qualified name
            UserDetails userDetails = buildUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            UserDto userDto = userMapper.toDto(user);

            return new AuthResponse(token, userDto);

        } catch (Exception e) {
            log.error("Error registering biometric for email: {}", email, e);
            throw new BiometricAuthException("Errore nella registrazione biometrica: " + e.getMessage(), e);
        }
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
     * Login with biometric
     */
    public AuthResponse loginBiometric(BiometricLoginRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato: " + email));

        Challenge challenge = challengeStore.remove(email);
        if (challenge == null) {
            throw new BiometricAuthException("Challenge scaduta o non trovata");
        }

        try {
            String credentialId = request.getAssertion().getId();
            UserCredential credential = userCredentialRepository.findByCredentialId(credentialId)
                    .orElseThrow(() -> new BiometricAuthException("Credenziale non trovata"));

            if (!credential.getUserId().equals(user.getId())) {
                throw new BiometricAuthException("Credenziale non appartiene all'utente");
            }

            // Decode assertion response
            byte[] clientDataJSON = Base64.getUrlDecoder()
                    .decode(request.getAssertion().getResponse().getClientDataJSON());
            byte[] authenticatorData = Base64.getUrlDecoder()
                    .decode(request.getAssertion().getResponse().getAuthenticatorData());
            byte[] signature = Base64.getUrlDecoder()
                    .decode(request.getAssertion().getResponse().getSignature());
            byte[] userHandle = request.getAssertion().getResponse().getUserHandle() != null
                    ? Base64.getUrlDecoder().decode(request.getAssertion().getResponse().getUserHandle())
                    : null;

            ServerProperty serverProperty = new ServerProperty(ORIGIN, RP_ID, challenge, null);
            AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                    Base64.getUrlDecoder().decode(credentialId),
                    userHandle,
                    authenticatorData,
                    clientDataJSON,
                    signature);

            AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                    serverProperty,
                    null,
                    false
            );

            AuthenticationData authData = webAuthnManager.validate(
                    authenticationRequest,
                    authenticationParameters);

            // Update sign count
            credential.setSignCount(authData.getAuthenticatorData().getSignCount());
            userCredentialRepository.save(credential);

            log.info("Biometric login successful for email: {}", email);

            // ✅ FIXED: Usa fully qualified name
            UserDetails userDetails = buildUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            UserDto userDto = userMapper.toDto(user);

            return new AuthResponse(token, userDto);

        } catch (BiometricAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing biometric login for email: {}", email, e);
            throw new BiometricAuthException("Errore nell'autenticazione biometrica: " + e.getMessage(), e);
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
