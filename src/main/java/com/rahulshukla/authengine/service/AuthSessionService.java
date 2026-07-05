package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.model.AuthSessionContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Per-user session cache for completed login orchestration.
 * <p>
 * The service prevents repeated post-login execution for the same username within a JVM
 * by storing the completed session context atomically.
 */
@Service
public class AuthSessionService {
    private final ConcurrentHashMap<String, AuthSessionContext> sessions = new ConcurrentHashMap<>();

    public AuthSessionContext findOrCreateCompletedSession(String username, Supplier<AuthSessionContext> sessionSupplier) {
        return findOrCreateCompletedSession("login", username, sessionSupplier);
    }

    public AuthSessionContext findOrCreateCompletedSession(String flowName, String username, Supplier<AuthSessionContext> sessionSupplier) {
        validateUsername(username);
        return sessions.computeIfAbsent(sessionKey(flowName, username), ignored -> sessionSupplier.get());
    }

    public Optional<AuthSessionContext> findByUsername(String username) {
        return findByFlowAndUsername("login", username);
    }

    public Optional<AuthSessionContext> findByFlowAndUsername(String flowName, String username) {
        return Optional.ofNullable(sessions.get(sessionKey(flowName, username)));
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }

    private String sessionKey(String flowName, String username) {
        return flowName + ":" + username;
    }
}
