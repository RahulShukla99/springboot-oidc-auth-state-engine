package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerJsonTest {

    @Test
    void shouldReturnJsonForHomeEndpointByDefault() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller()).build();

        mockMvc.perform(get("/").accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.loginUrl").value("/oauth2/authorization/auth0"));
    }

    @Test
    void shouldExposeConfiguredFlowAsJson() {
        AuthController controller = controller();

        var flow = controller.flow();

        assertThat(flow.flowName()).isEqualTo("test-flow");
        assertThat(flow.initialState()).isEqualTo("START");
        assertThat(flow.finalStates()).isEmpty();
        assertThat(flow.transitions()).isEmpty();
    }

    @Test
    void shouldReturnUnauthenticatedSessionSnapshotWhenNoPrincipalIsPresent() {
        AuthController controller = controller();

        var session = controller.session(null, null);

        assertThat(session.authenticated()).isFalse();
        assertThat(session.username()).isNull();
        assertThat(session.currentState()).isNull();
        assertThat(session.finalState()).isNull();
    }

    private AuthController controller() {
        AuthFlow flow = new AuthFlow("test-flow", List.of(new AuthState("START", true, false, List.of())));
        return new AuthController(
                new AuthStateEngine(flow, new InMemoryAuditService()),
                new AuthorizationService(List.of("APP_USER")),
                new AuthSessionService(),
                new InMemoryAuditService(),
                flow
        );
    }
}
