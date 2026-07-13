package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.model.AuthSessionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTransitionRuleResolverTest {

    private final DefaultTransitionRuleResolver resolver = new DefaultTransitionRuleResolver();

    @Test
    void shouldResolveGuardedRulesForKnownEventsAndDefaultForUnknownEvents() {
        AuthSessionContext verified = new AuthSessionContext("corr-1");
        verified.setEmailVerified(true);
        verified.setFailureReason("allowed");
        verified.setMfaPassed(true);
        AuthSessionContext unverified = new AuthSessionContext("corr-2");
        unverified.setEmailVerified(false);
        unverified.setFailureReason(null);
        unverified.setMfaPassed(false);

        assertThat(resolver.resolve("flow", "LOGIN_REQUESTED").matches(verified)).isTrue();
        assertThat(resolver.resolve("flow", "OIDC_CALLBACK_RECEIVED").matches(verified)).isTrue();
        assertThat(resolver.resolve("flow", "PROFILE_LOADED").matches(verified)).isTrue();
        assertThat(resolver.resolve("flow", "TOKEN_VALID").matches(verified)).isTrue();
        assertThat(resolver.resolve("flow", "TOKEN_VALID").matches(unverified)).isFalse();
        assertThat(resolver.resolve("flow", "TOKEN_INVALID").matches(unverified)).isTrue();
        assertThat(resolver.resolve("flow", "TOKEN_INVALID").matches(verified)).isFalse();
        AuthSessionContext denied = new AuthSessionContext("corr-3");
        denied.setFailureReason("denied");
        AuthSessionContext blankReason = new AuthSessionContext("corr-4");
        blankReason.setFailureReason(" ");
        assertThat(resolver.resolve("flow", "USER_AUTHORIZED").matches(denied)).isFalse();
        assertThat(resolver.resolve("flow", "USER_AUTHORIZED").matches(blankReason)).isTrue();
        assertThat(resolver.resolve("flow", "USER_NOT_AUTHORIZED").matches(denied)).isTrue();
        assertThat(resolver.resolve("flow", "USER_NOT_AUTHORIZED").matches(blankReason)).isFalse();
        assertThat(resolver.resolve("flow", "MFA_PASSED").matches(verified)).isTrue();
        assertThat(resolver.resolve("flow", "MFA_FAILED").matches(unverified)).isTrue();
        assertThat(resolver.resolve("flow", "MFA_FAILED").matches(verified)).isFalse();
        assertThat(resolver.resolve("flow", "UNKNOWN_EVENT").matches(verified)).isTrue();
    }
}
