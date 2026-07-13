package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.exception.AuthFlowValidationException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowRegistryBehaviorTest {

    @Test
    void shouldRejectMissingRegistryConfiguration() {
        assertThatThrownBy(() -> new AuthFlowRegistry(null))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessage("At least one authentication flow must be configured");
        assertThatThrownBy(() -> new AuthFlowRegistry(Map.of()))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessage("At least one authentication flow must be configured");
    }

    @Test
    void shouldExposeUnmodifiableFlowMap() {
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of("login", new AuthStateEngine(flow(), new InMemoryAuditService(100))));

        assertThatThrownBy(() -> registry.enginesByName().put("other", new AuthStateEngine(flow(), new InMemoryAuditService(100))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private AuthFlow flow() {
        return new AuthFlow("flow", List.of(new AuthState("START", true, false, List.of())));
    }
}
