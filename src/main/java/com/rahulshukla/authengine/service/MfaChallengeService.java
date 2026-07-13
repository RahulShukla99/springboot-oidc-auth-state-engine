package com.rahulshukla.authengine.service;

public interface MfaChallengeService {
    String issueChallenge(String username);

    boolean verifyChallenge(String username, String code);
}
