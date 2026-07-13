package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthStateEngineAuditTest {

    @Test
    void shouldPreserveExistingFailureReasonWhenTokenValidationFails() {
        AuthStateEngine engine = new AuthStateEngine(fullFlow(), new InMemoryAuditService(100));
        AuthSessionContext context = new AuthSessionContext("corr-missing-email");
        context.setFailureReason("OIDC user email is missing");

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getFailureReason()).isEqualTo("OIDC user email is missing");
    }

    @Test
    void shouldAuditFailureOutcomeWhenAuthorizationFails() {
        InMemoryAuditService auditService = new InMemoryAuditService(100);
        AuthStateEngine engine = new AuthStateEngine(fullFlow(), auditService);
        AuthSessionContext context = new AuthSessionContext("corr-1");
        context.setUsername("user@example.com");
        context.setEmailVerified(true);
        context.setFailureReason("OIDC user does not belong to an allowed group");

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(auditService.recentRecords()).last()
                .extracting("fromState", "event", "toState", "outcome")
                .containsExactly("AUTHORIZE_USER", "USER_NOT_AUTHORIZED", "AUTH_FAILED", "FAILURE");
    }

    private AuthFlow fullFlow() {
        return new AuthFlow("full-flow", List.of(
                new AuthState("START", true, false, List.of(new AuthTransition("LOGIN_REQUESTED", "REDIRECT_TO_IDP"))),
                new AuthState("REDIRECT_TO_IDP", false, false, List.of(new AuthTransition("OIDC_CALLBACK_RECEIVED", "VALIDATE_TOKEN"))),
                new AuthState("VALIDATE_TOKEN", false, false, List.of(
                        new AuthTransition("TOKEN_VALID", "LOAD_USER_PROFILE"),
                        new AuthTransition("TOKEN_INVALID", "AUTH_FAILED"))),
                new AuthState("LOAD_USER_PROFILE", false, false, List.of(new AuthTransition("PROFILE_LOADED", "AUTHORIZE_USER"))),
                new AuthState("AUTHORIZE_USER", false, false, List.of(
                        new AuthTransition("USER_AUTHORIZED", "AUTH_SUCCESS"),
                        new AuthTransition("USER_NOT_AUTHORIZED", "AUTH_FAILED"))),
                new AuthState("AUTH_SUCCESS", false, true, List.of()),
                new AuthState("AUTH_FAILED", false, true, List.of())
        ));
    }
}
