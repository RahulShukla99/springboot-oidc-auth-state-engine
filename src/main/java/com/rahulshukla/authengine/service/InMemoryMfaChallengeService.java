package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.config.AuthMfaProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryMfaChallengeService implements MfaChallengeService {
    private final String challengeCode;
    private final ConcurrentHashMap<String, String> challengesByUsername = new ConcurrentHashMap<>();

    public InMemoryMfaChallengeService(AuthMfaProperties properties) {
        this.challengeCode = properties == null || properties.challengeCode() == null || properties.challengeCode().isBlank()
                ? null
                : properties.challengeCode();
        if (challengeCode == null) {
            throw new IllegalArgumentException("auth.mfa.challenge-code must not be blank");
        }
    }

    @Override
    public String issueChallenge(String username) {
        validateUsername(username);
        return challengesByUsername.computeIfAbsent(username, ignored -> challengeCode);
    }

    @Override
    public boolean verifyChallenge(String username, String code) {
        validateUsername(username);
        if (code == null || code.isBlank()) {
            return false;
        }
        return issueChallenge(username).equals(code);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
