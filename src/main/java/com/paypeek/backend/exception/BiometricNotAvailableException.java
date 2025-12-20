package com.paypeek.backend.exception;

public class BiometricNotAvailableException extends RuntimeException {
    public BiometricNotAvailableException(String message) {
        super(message);
    }
}