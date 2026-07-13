package com.rahulshukla.authengine.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMfaChallengeServiceTest {

    @Test
    void shouldReturnIssuedCodeAndVerifyItForTheSameUser() {
        InMemoryMfaChallengeService service = new InMemoryMfaChallengeService(new com.rahulshukla.authengine.config.AuthMfaProperties("654321"));

        String code = service.issueChallenge("user@example.com");

        assertThat(code).isEqualTo("654321");
        assertThat(service.verifyChallenge("user@example.com", code)).isTrue();
    }

    @Test
    void shouldRejectBlankCodeAndBlankUsername() {
        InMemoryMfaChallengeService service = new InMemoryMfaChallengeService(new com.rahulshukla.authengine.config.AuthMfaProperties("654321"));

        assertThat(service.verifyChallenge("user@example.com", "")).isFalse();
        assertThat(service.verifyChallenge("user@example.com", null)).isFalse();
        assertThatThrownBy(() -> service.verifyChallenge(null, "654321"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
        assertThatThrownBy(() -> service.issueChallenge(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
        assertThatThrownBy(() -> service.issueChallenge(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
        assertThatThrownBy(() -> new InMemoryMfaChallengeService(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.mfa.challenge-code must not be blank");
        assertThatThrownBy(() -> new InMemoryMfaChallengeService(new com.rahulshukla.authengine.config.AuthMfaProperties(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.mfa.challenge-code must not be blank");
        assertThatThrownBy(() -> new InMemoryMfaChallengeService(new com.rahulshukla.authengine.config.AuthMfaProperties(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.mfa.challenge-code must not be blank");
    }
}
