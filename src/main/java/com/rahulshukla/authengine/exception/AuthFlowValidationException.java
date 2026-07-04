package com.rahulshukla.authengine.exception;

public class AuthFlowValidationException extends RuntimeException {
    public AuthFlowValidationException(String message) {
        super(message);
    }

    public AuthFlowValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
