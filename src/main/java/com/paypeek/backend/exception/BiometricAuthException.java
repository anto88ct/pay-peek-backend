package com.paypeek.backend.exception;

public class BiometricAuthException extends RuntimeException {
    public BiometricAuthException(String message) {
        super(message);
    }

    public BiometricAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}