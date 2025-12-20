package com.paypeek.backend.exception;

import com.paypeek.backend.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Email già registrata (esempio custom exception)
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex,
            HttpServletRequest request
    ) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .type("client")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Non autenticato (es. token mancante/non valido) – se hai una UnauthorizedException custom
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .type("client")
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // Accesso negato da Spring Security (ruolo/permessi) -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .type("client")
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message("Non hai i permessi per accedere a questa risorsa.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Errori di validazione @Valid su DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String defaultMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Dati non validi");

        ErrorResponseDto body = ErrorResponseDto.builder()
                .type("validation")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(defaultMessage)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Fallback per tutte le altre RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponseDto body = ErrorResponseDto.builder()
                .type("server")
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Si è verificato un errore interno.")
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
