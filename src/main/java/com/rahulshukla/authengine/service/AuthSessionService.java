package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.model.AuthSessionContext;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthSessionService {
    private final ConcurrentHashMap<String, AuthSessionContext> sessions = new ConcurrentHashMap<>();

    public AuthSessionContext findOrCreateCompletedSession(String username, Supplier<AuthSessionContext> sessionSupplier) {
        return findOrCreateCompletedSession("login", username, sessionSupplier);
    }

    /**
     * Returns the cached session for a flow and user or creates it once atomically.
     */
    public AuthSessionContext findOrCreateCompletedSession(String flowName, String username, Supplier<AuthSessionContext> sessionSupplier) {
        validateUsername(username);
        String key = sessionKey(flowName, username);
        log.debug("session cache access flow={} username={}", flowName, username);
        return sessions.computeIfAbsent(key, ignored -> sessionSupplier.get());
    }

    public Optional<AuthSessionContext> findByUsername(String username) {
        return findByFlowAndUsername("login", username);
    }

    /**
     * Looks up a cached session for the given flow and user.
     */
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
