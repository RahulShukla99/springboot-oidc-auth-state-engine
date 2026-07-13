package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.config.AuthMfaProperties;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.service.AuthorizationService;
import com.rahulshukla.authengine.service.FlowDiagramService;
import com.rahulshukla.authengine.service.InMemoryMfaChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowViewControllerTest {

    @Test
    void shouldRenderInlineGraphvizFlowViewForDefaultWorkflow() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller()).build();

        mockMvc.perform(get("/auth/flow/view").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<main class=\"diagram-shell\">")))
                .andExpect(content().string(containsString("<figure class=\"diagram-frame\"")))
                .andExpect(content().string(containsString("data-renderer=\"graphviz\"")))
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("Spring Boot OIDC Authentication State Engine")))
                .andExpect(content().string(containsString("TOKEN_INVALID")))
                .andExpect(content().string(not(containsString("<img"))))
                .andExpect(content().string(not(containsString("data:image/svg+xml;base64,"))))
                .andExpect(content().string(not(containsString("<?xml"))))
                .andExpect(content().string(not(containsString("mermaid"))));
    }

    @Test
    void shouldRenderInlineGraphvizFlowViewForStepUpWorkflow() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller()).build();

        mockMvc.perform(get("/auth/flow/step-up/view").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<main class=\"diagram-shell\">")))
                .andExpect(content().string(containsString("<figure class=\"diagram-frame\"")))
                .andExpect(content().string(containsString("data-renderer=\"graphviz\"")))
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("Spring Boot OIDC Authentication State Engine")))
                .andExpect(content().string(containsString("MFA_FAILED")))
                .andExpect(content().string(not(containsString("<img"))))
                .andExpect(content().string(not(containsString("data:image/svg+xml;base64,"))))
                .andExpect(content().string(not(containsString("<?xml"))))
                .andExpect(content().string(not(containsString("stateDiagram-v2"))));
    }

    private AuthFlowViewController controller() {
        AuthFlow loginFlow = new AuthFlow("oidc-post-login-auth-flow", List.of(
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
        AuthFlow stepUpFlow = new AuthFlow("step-up-mfa-flow", List.of(
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
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of(
                "login", new AuthStateEngine(loginFlow, new InMemoryAuditService(100)),
                "step-up", new AuthStateEngine(stepUpFlow, new InMemoryAuditService(100))
        ));
        return new AuthFlowViewController(registry, new AuthViewMapper(), new FlowDiagramService());
    }
}
