package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.exception.AuthStateException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStateEngineTest {

    @Test
    void shouldExecuteValidTransition() {
        AuthStateEngine engine = new AuthStateEngine(flow(), new InMemoryAuditService());

        AuthState next = engine.transition("START", "LOGIN_REQUESTED");

        assertThat(next.id()).isEqualTo("REDIRECT_TO_IDP");
    }

    @Test
    void shouldRejectInvalidEvent() {
        AuthStateEngine engine = new AuthStateEngine(flow(), new InMemoryAuditService());

        assertThatThrownBy(() -> engine.transition("START", "TOKEN_VALID"))
                .isInstanceOf(AuthStateException.class)
                .hasMessageContaining("Event TOKEN_VALID is not allowed from state START");
    }

    private AuthFlow flow() {
        return new AuthFlow("test-flow", List.of(
                new AuthState("START", true, false, List.of(new AuthTransition("LOGIN_REQUESTED", "REDIRECT_TO_IDP"))),
                new AuthState("REDIRECT_TO_IDP", false, false, List.of()),
                new AuthState("AUTH_SUCCESS", false, true, List.of())
        ));
    }
}
