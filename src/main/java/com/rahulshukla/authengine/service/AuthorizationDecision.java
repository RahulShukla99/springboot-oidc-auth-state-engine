package com.rahulshukla.authengine.service;

public record AuthorizationDecision(boolean authorized, String reason) {
    public static AuthorizationDecision allow(String reason) {
        return new AuthorizationDecision(true, reason);
    }

    public static AuthorizationDecision deny(String reason) {
        return new AuthorizationDecision(false, reason);
    }
}
