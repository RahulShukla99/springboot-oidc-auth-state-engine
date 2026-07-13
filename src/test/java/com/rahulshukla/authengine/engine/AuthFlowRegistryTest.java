package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.exception.AuthFlowValidationException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowRegistryTest {

    @Test
    void shouldReturnNamedStateEngine() {
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of(
                "login", new AuthStateEngine(flow("login-flow"), new InMemoryAuditService(100))
        ));

        AuthStateEngine engine = registry.getRequiredEngine("login");

        assertThat(engine.getInitialState().id()).isEqualTo("START");
    }

    @Test
    void shouldRejectUnknownFlowName() {
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of(
                "login", new AuthStateEngine(flow("login-flow"), new InMemoryAuditService(100))
        ));

        assertThatThrownBy(() -> registry.getRequiredEngine("step-up"))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessage("Authentication flow is not configured: step-up");
    }

    private AuthFlow flow(String name) {
        return new AuthFlow(name, List.of(new AuthState("START", true, false, List.of())));
    }
}
