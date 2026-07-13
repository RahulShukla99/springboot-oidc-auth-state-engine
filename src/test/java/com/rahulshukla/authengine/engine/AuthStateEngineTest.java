package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.exception.AuthStateException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStateEngineTest {

    @Test
    void shouldExecuteValidTransition() {
        AuthStateEngine engine = new AuthStateEngine(flow(), new InMemoryAuditService(100));

        AuthState next = engine.transition("START", "LOGIN_REQUESTED");

        assertThat(next.id()).isEqualTo("REDIRECT_TO_IDP");
    }

    @Test
    void shouldRejectInvalidEvent() {
        AuthStateEngine engine = new AuthStateEngine(flow(), new InMemoryAuditService(100));

        assertThatThrownBy(() -> engine.transition("START", "TOKEN_VALID"))
                .isInstanceOf(AuthStateException.class)
                .hasMessageContaining("Event TOKEN_VALID is not allowed from state START");
    }

    @Test
    void shouldExecuteStepUpFlowEndToEndWhenMfaPasses() {
        InMemoryAuditService auditService = new InMemoryAuditService(100);
        AuthStateEngine engine = new AuthStateEngine(stepUpFlow(), auditService);
        AuthSessionContext context = new AuthSessionContext("corr-step-up");
        context.setUsername("user@example.com");
        context.setEmailVerified(true);
        context.setMfaPassed(true);

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getFinalState()).isEqualTo("STEP_UP_SUCCESS");
        assertThat(result.getTransitionHistory()).extracting("event").contains("MFA_PASSED");
        assertThat(auditService.recentRecords()).extracting("event").contains("MFA_PASSED");
    }

    @Test
    void shouldExecuteStepUpFlowEndToEndWhenMfaFails() {
        InMemoryAuditService auditService = new InMemoryAuditService(100);
        AuthStateEngine engine = new AuthStateEngine(stepUpFlow(), auditService);
        AuthSessionContext context = new AuthSessionContext("corr-step-up-fail");
        context.setUsername("user@example.com");
        context.setEmailVerified(true);
        context.setMfaPassed(false);

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getTransitionHistory()).extracting("event").contains("MFA_FAILED");
        assertThat(auditService.recentRecords()).extracting("event").contains("MFA_FAILED");
    }

    @Test
    void shouldPreserveExistingFailureReasonWhenEmailIsNotVerified() {
        InMemoryAuditService auditService = new InMemoryAuditService(100);
        AuthStateEngine engine = new AuthStateEngine(stepUpFlow(), auditService);
        AuthSessionContext context = new AuthSessionContext("corr-failure-reason");
        context.setUsername("user@example.com");
        context.setEmailVerified(false);
        context.setFailureReason("manual review required");
        context.setMfaPassed(false);

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getFailureReason()).isEqualTo("manual review required");
        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
    }

    @Test
    void shouldRejectFlowExecutionWhenNoTransitionGuardMatches() {
        AuthStateEngine engine = new AuthStateEngine(stepUpFlow(), new InMemoryAuditService(100), (flowName, event) -> TransitionRule.failure(context -> false));
        AuthSessionContext context = new AuthSessionContext("corr-no-match");
        context.setUsername("user@example.com");
        context.setEmailVerified(true);

        assertThatThrownBy(() -> engine.executeLoginFlow(context))
                .isInstanceOf(AuthStateException.class)
                .hasMessageContaining("No transition guard matched from state START");
    }

    @Test
    void shouldRejectTransitionWhenTargetStateIsMissing() {
        AuthFlow flow = new AuthFlow("broken-flow", List.of(
                new AuthState("START", true, false, List.of(new AuthTransition("GO", "MISSING")))
        ));
        AuthStateEngine engine = new AuthStateEngine(flow, new InMemoryAuditService(100));

        assertThatThrownBy(() -> engine.transition("START", "GO"))
                .isInstanceOf(AuthStateException.class)
                .hasMessageContaining("Target state does not exist: MISSING");
    }

    @Test
    void shouldRejectTransitionWhenCurrentStateIsMissing() {
        AuthStateEngine engine = new AuthStateEngine(flow(), new InMemoryAuditService(100));

        assertThatThrownBy(() -> engine.transition("MISSING", "GO"))
                .isInstanceOf(AuthStateException.class)
                .hasMessageContaining("Current state does not exist: MISSING");
    }

    @Test
    void shouldReturnImmediatelyWhenStartingFromFinalState() {
        AuthFlow flow = new AuthFlow("final-only", List.of(
                new AuthState("AUTH_SUCCESS", true, true, List.of())
        ));
        AuthStateEngine engine = new AuthStateEngine(flow, new InMemoryAuditService(100));
        AuthSessionContext context = new AuthSessionContext("corr-final");
        context.setUsername("user@example.com");

        AuthSessionContext result = engine.executeLoginFlow(context);

        assertThat(result.getCurrentState()).isEqualTo("AUTH_SUCCESS");
        assertThat(result.getFinalState()).isNull();
        assertThat(result.getTransitionHistory()).isEmpty();
    }

    private AuthFlow flow() {
        return new AuthFlow("test-flow", List.of(
                new AuthState("START", true, false, List.of(new AuthTransition("LOGIN_REQUESTED", "REDIRECT_TO_IDP"))),
                new AuthState("REDIRECT_TO_IDP", false, false, List.of()),
                new AuthState("AUTH_SUCCESS", false, true, List.of())
        ));
    }

    private AuthFlow stepUpFlow() {
        return new AuthFlow("step-up-mfa-flow", List.of(
                new AuthState("START", true, false, List.of(new AuthTransition("LOGIN_REQUESTED", "REDIRECT_TO_IDP"))),
                new AuthState("REDIRECT_TO_IDP", false, false, List.of(new AuthTransition("OIDC_CALLBACK_RECEIVED", "VALIDATE_TOKEN"))),
                new AuthState("VALIDATE_TOKEN", false, false, List.of(
                        new AuthTransition("TOKEN_VALID", "LOAD_USER_PROFILE"),
                        new AuthTransition("TOKEN_INVALID", "AUTH_FAILED"))),
                new AuthState("LOAD_USER_PROFILE", false, false, List.of(new AuthTransition("PROFILE_LOADED", "REQUIRE_MFA"))),
                new AuthState("REQUIRE_MFA", false, false, List.of(
                        new AuthTransition("MFA_PASSED", "STEP_UP_SUCCESS"),
                        new AuthTransition("MFA_FAILED", "AUTH_FAILED"))),
                new AuthState("STEP_UP_SUCCESS", false, true, List.of()),
                new AuthState("AUTH_FAILED", false, true, List.of())
        ));
    }
}
