package com.paypeek.backend.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Non sei autorizzato ad accedere a questa risorsa.");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
