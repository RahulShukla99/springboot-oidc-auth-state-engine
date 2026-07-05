package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
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
import java.util.Map;

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
    void shouldExposeNamedStepUpFlowAsJson() {
        AuthController controller = controller();

        var flow = controller.flow("step-up");

        assertThat(flow.flowName()).isEqualTo("step-up-mfa-flow");
        assertThat(flow.initialState()).isEqualTo("START");
        assertThat(flow.finalStates()).containsExactly("STEP_UP_SUCCESS", "AUTH_FAILED");
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
        AuthFlow loginFlow = new AuthFlow("test-flow", List.of(new AuthState("START", true, false, List.of())));
        AuthFlow stepUpFlow = new AuthFlow("step-up-mfa-flow", List.of(
                new AuthState("START", true, false, List.of(new com.rahulshukla.authengine.model.AuthTransition("LOGIN_REQUESTED", "REDIRECT_TO_IDP"))),
                new AuthState("REDIRECT_TO_IDP", false, false, List.of(new com.rahulshukla.authengine.model.AuthTransition("OIDC_CALLBACK_RECEIVED", "VALIDATE_TOKEN"))),
                new AuthState("VALIDATE_TOKEN", false, false, List.of(
                        new com.rahulshukla.authengine.model.AuthTransition("TOKEN_VALID", "LOAD_USER_PROFILE"),
                        new com.rahulshukla.authengine.model.AuthTransition("TOKEN_INVALID", "AUTH_FAILED"))),
                new AuthState("LOAD_USER_PROFILE", false, false, List.of(new com.rahulshukla.authengine.model.AuthTransition("PROFILE_LOADED", "REQUIRE_MFA"))),
                new AuthState("REQUIRE_MFA", false, false, List.of(
                        new com.rahulshukla.authengine.model.AuthTransition("MFA_PASSED", "STEP_UP_SUCCESS"),
                        new com.rahulshukla.authengine.model.AuthTransition("MFA_FAILED", "AUTH_FAILED"))),
                new AuthState("STEP_UP_SUCCESS", false, true, List.of()),
                new AuthState("AUTH_FAILED", false, true, List.of())
        ));
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of(
                "login", new AuthStateEngine(loginFlow, new InMemoryAuditService()),
                "step-up", new AuthStateEngine(stepUpFlow, new InMemoryAuditService())
        ));
        return new AuthController(
                registry,
                new AuthorizationService(List.of("APP_USER")),
                new AuthSessionService(),
                new InMemoryAuditService()
        );
    }
}
