package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.model.AuthSessionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionRuleTest {

    @Test
    void shouldDefaultNullGuardAndBlankOutcomeToSuccess() {
        TransitionRule rule = new TransitionRule(null, " ");

        assertThat(rule.outcome()).isEqualTo("SUCCESS");
        assertThat(rule.matches(new AuthSessionContext("corr-1"))).isTrue();
    }

    @Test
    void shouldCreateSuccessAndFailureRules() {
        AuthSessionContext verified = new AuthSessionContext("corr-2");
        verified.setEmailVerified(true);
        AuthSessionContext unverified = new AuthSessionContext("corr-3");
        unverified.setEmailVerified(false);

        assertThat(TransitionRule.success(AuthSessionContext::isEmailVerified).matches(verified)).isTrue();
        assertThat(TransitionRule.failure(AuthSessionContext::isEmailVerified).matches(unverified)).isFalse();
        assertThat(TransitionRule.failure(AuthSessionContext::isEmailVerified).outcome()).isEqualTo("FAILURE");
        assertThat(new TransitionRule(AuthSessionContext::isEmailVerified, "CUSTOM").outcome()).isEqualTo("CUSTOM");
        assertThat(new TransitionRule(null, null).matches(verified)).isTrue();
    }
}
