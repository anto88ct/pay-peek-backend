package com.paypeek.backend.controller;

import com.paypeek.backend.dto.*;
import com.paypeek.backend.exception.BiometricAuthException;
import com.paypeek.backend.exception.BiometricNotAvailableException;
import com.paypeek.backend.exception.UserNotFoundException;
import com.paypeek.backend.service.BiometricService;
import com.paypeek.backend.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/biometric")
@RequiredArgsConstructor
@Slf4j
public class BiometricController {

    private final BiometricService biometricService;
    private final AuditLogger auditLogger;

    @GetMapping("/register-challenge/{email}")
    public ResponseEntity<?> getRegisterChallenge(@PathVariable String email) {
        try {
            log.info("🔵 [BIOMETRIC] Generating register challenge for email: {}", email);

            PublicKeyCredentialCreationOptions options = biometricService.generateRegisterChallenge(email);

            log.info("✅ [BIOMETRIC] Register challenge generated successfully");
            auditLogger.log(email, "BIOMETRIC_REGISTER_CHALLENGE_GENERATED", "success");

            return ResponseEntity.ok(options);

        } catch (UserNotFoundException e) {
            log.warn("⚠️ [BIOMETRIC] User not found: {}", email);
            auditLogger.log(email, "BIOMETRIC_REGISTER_CHALLENGE_FAILED", "user_not_found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("USER_NOT_FOUND", "Utente non trovato", HttpStatus.NOT_FOUND.value()));

        } catch (BiometricNotAvailableException e) {
            log.warn("⚠️ [BIOMETRIC] Biometric not available: {}", email);
            auditLogger.log(email, "BIOMETRIC_REGISTER_CHALLENGE_FAILED", "biometric_not_available");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("BIOMETRIC_NOT_AVAILABLE", e.getMessage(), HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Error generating register challenge:", e);
            auditLogger.log(email, "BIOMETRIC_REGISTER_CHALLENGE_ERROR", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SERVER_ERROR", "Errore nella generazione della challenge", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerBiometric(
            @Valid @RequestBody BiometricRegisterRequest request) {
        try {
            log.info("🔵 [BIOMETRIC] Registering biometric credential for email: {}", request.getEmail());

            AuthResponse response = biometricService.registerBiometric(request);

            log.info("✅ [BIOMETRIC] Biometric registered successfully for: {}", request.getEmail());
            auditLogger.log(request.getEmail(), "BIOMETRIC_REGISTERED", "success");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserNotFoundException e) {
            log.warn("⚠️ [BIOMETRIC] User not found: {}", request.getEmail());
            auditLogger.log(request.getEmail(), "BIOMETRIC_REGISTER_FAILED", "user_not_found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("USER_NOT_FOUND", "Utente non trovato", HttpStatus.NOT_FOUND.value()));

        } catch (BiometricAuthException e) {
            log.warn("⚠️ [BIOMETRIC] Biometric authentication failed: {}", e.getMessage());
            auditLogger.log(request.getEmail(), "BIOMETRIC_REGISTER_FAILED", "invalid_credential");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("INVALID_CREDENTIAL", e.getMessage(), HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Error registering biometric:", e);
            auditLogger.log(request.getEmail(), "BIOMETRIC_REGISTER_ERROR", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SERVER_ERROR", "Errore nella registrazione biometrica", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/login-challenge/{email}")
    public ResponseEntity<?> getLoginChallenge(@PathVariable String email) {
        try {
            log.info("🔵 [BIOMETRIC] Generating login challenge for email: {}", email);

            PublicKeyCredentialRequestOptions options = biometricService.generateLoginChallenge(email);

            log.info("✅ [BIOMETRIC] Login challenge generated successfully");
            auditLogger.log(email, "BIOMETRIC_LOGIN_CHALLENGE_GENERATED", "success");

            return ResponseEntity.ok(options);

        } catch (UserNotFoundException e) {
            log.warn("⚠️ [BIOMETRIC] User not found: {}", email);
            auditLogger.log(email, "BIOMETRIC_LOGIN_CHALLENGE_FAILED", "user_not_found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("USER_NOT_FOUND", "Utente non trovato", HttpStatus.NOT_FOUND.value()));

        } catch (BiometricNotAvailableException e) {
            log.warn("⚠️ [BIOMETRIC] Biometric not registered: {}", email);
            auditLogger.log(email, "BIOMETRIC_LOGIN_CHALLENGE_FAILED", "biometric_not_registered");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("BIOMETRIC_NOT_REGISTERED", e.getMessage(), HttpStatus.BAD_REQUEST.value()));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Error generating login challenge:", e);
            auditLogger.log(email, "BIOMETRIC_LOGIN_CHALLENGE_ERROR", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SERVER_ERROR", "Errore nella generazione della challenge", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginBiometric(
            @Valid @RequestBody BiometricLoginRequest request) {
        try {
            log.info("🔵 [BIOMETRIC] Processing biometric login for email: {}", request.getEmail());

            AuthResponse response = biometricService.loginBiometric(request);

            log.info("✅ [BIOMETRIC] Biometric login successful for: {}", request.getEmail());
            auditLogger.log(request.getEmail(), "BIOMETRIC_LOGIN_SUCCESS", "success");

            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            log.warn("⚠️ [BIOMETRIC] User not found: {}", request.getEmail());
            auditLogger.log(request.getEmail(), "BIOMETRIC_LOGIN_FAILED", "user_not_found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("USER_NOT_FOUND", "Utente non trovato", HttpStatus.NOT_FOUND.value()));

        } catch (BiometricAuthException e) {
            log.warn("⚠️ [BIOMETRIC] Biometric authentication failed: {}", e.getMessage());
            auditLogger.log(request.getEmail(), "BIOMETRIC_LOGIN_FAILED", "auth_failed");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("BIOMETRIC_AUTH_FAILED", e.getMessage(), HttpStatus.UNAUTHORIZED.value()));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Error processing biometric login:", e);
            auditLogger.log(request.getEmail(), "BIOMETRIC_LOGIN_ERROR", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SERVER_ERROR", "Errore nell'autenticazione biometrica", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/check/{email}")
    public ResponseEntity<?> checkBiometricRegistration(@PathVariable String email) {
        try {
            log.info("🔵 [BIOMETRIC] Checking biometric registration for email: {}", email);

            boolean isRegistered = biometricService.isBiometricRegistered(email);

            log.info("✅ [BIOMETRIC] Biometric check result: {}", isRegistered);

            return ResponseEntity.ok(new BiometricCheckResponse(isRegistered));

        } catch (Exception e) {
            log.error("❌ [BIOMETRIC] Error checking biometric registration:", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildErrorResponse("SERVER_ERROR", "Errore nella verifica della biometria", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    private ErrorResponseDto buildErrorResponse(String type, String message, int statusCode) {
        String path = "/api/auth/biometric";

        // Prova a ottenere il path dall'HttpRequest se disponibile
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (request != null) {
                path = request.getRequestURI();
            }
        } catch (Exception e) {
            log.debug("Could not get request URI from context");
        }

        return ErrorResponseDto.builder()
                .type(type)
                .statusCode(statusCode)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}