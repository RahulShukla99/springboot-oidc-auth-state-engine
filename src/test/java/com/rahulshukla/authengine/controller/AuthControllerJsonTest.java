package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import com.rahulshukla.authengine.config.AuthMfaProperties;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.service.AuthorizationService;
import com.rahulshukla.authengine.service.InMemoryMfaChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThat(flow.flowName()).isEqualTo("oidc-post-login-auth-flow");
        assertThat(flow.initialState()).isEqualTo("START");
        assertThat(flow.finalStates()).containsExactly("AUTH_SUCCESS", "AUTH_FAILED");
        assertThat(flow.transitions()).hasSize(7);
    }

    @Test
    void shouldExecuteLoginFlowWithoutPrincipalWhenNoUserIsPresent() {
        AuthController controller = controller();

        var result = controller.success((OidcUser) null);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getFailureReason()).isEqualTo("OIDC user is not authenticated");
    }

    @Test
    void shouldExecuteLoginFlowForAuthorizedUser() {
        AuthController controller = controller();

        var result = controller.success(oidcUser());

        assertThat(result.getFinalState()).isEqualTo("AUTH_SUCCESS");
    }

    @Test
    void shouldReuseCompletedLoginSessionForSameUser() {
        AuthController controller = controller();

        var first = controller.success(oidcUser());
        var second = controller.success(oidcUser());

        assertThat(second.getCorrelationId()).isEqualTo(first.getCorrelationId());
        assertThat(second.getFinalState()).isEqualTo("AUTH_SUCCESS");
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

    @Test
    void shouldReturnAuthenticatedSessionSnapshotWhenPrincipalIsPresent() {
        AuthController controller = controller();
        controller.success(oidcUser());

        var authentication = new org.springframework.security.authentication.TestingAuthenticationToken("principal", "credentials");
        authentication.setAuthenticated(true);
        var session = controller.session(null, authentication, oidcUser());

        assertThat(session.authenticated()).isTrue();
        assertThat(session.username()).isEqualTo("user@example.com");
    }

    @Test
    void shouldReturnAuditTrail() {
        AuthController controller = controller();
        controller.verifyStepUp("123456", oidcUser());

        assertThat(controller.audit()).isNotEmpty();
    }

    @Test
    void shouldRejectBlankStepUpCode() {
        AuthController controller = controller();

        assertThatThrownBy(() -> controller.verifyStepUp(" ", oidcUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    void shouldReturnStepUpSuccessWhenMfaVerificationPasses() throws Exception {
        AuthController controller = controller();

        var result = controller.verifyStepUp("123456", oidcUser());

        assertThat(result.getFinalState()).isEqualTo("STEP_UP_SUCCESS");
        assertThat(result.getTransitionHistory()).hasSize(5);
        assertThat(result.getTransitionHistory()).last().extracting("event").isEqualTo("MFA_PASSED");
    }

    @Test
    void shouldReturnAuthFailedWhenStepUpAuthorizationFails() {
        AuthController controller = controller();
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", false, "sub", "auth0|test-user"));

        var result = controller.verifyStepUp("123456", user);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getFailureReason()).isEqualTo("OIDC user email is not verified");
    }

    @Test
    void shouldKeepAuthorizationFailureWhenMfaAlsoFails() {
        AuthController controller = controller();
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", false, "sub", "auth0|test-user"));

        var result = controller.verifyStepUp("999999", user);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getFailureReason()).isEqualTo("OIDC user email is not verified");
    }

    @Test
    void shouldAllowBlankEmailToTakeTheLoginFlowPath() {
        AuthController controller = controller();
        OidcUser user = oidcUser(Map.of("email", " ", "email_verified", true, "sub", "auth0|test-user"));

        var result = controller.success(user);

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getFailureReason()).isEqualTo("OIDC user email is missing");
    }

    @Test
    void shouldReturnAuthFailedWhenMfaVerificationFails() throws Exception {
        AuthController controller = controller();

        var result = controller.verifyStepUp("wrong-code", oidcUser());

        assertThat(result.getFinalState()).isEqualTo("AUTH_FAILED");
        assertThat(result.getTransitionHistory()).hasSize(5);
        assertThat(result.getTransitionHistory()).last().extracting("event").isEqualTo("MFA_FAILED");
    }

    @Test
    void shouldRejectStepUpForBlankEmailBeforeMfaVerification() {
        AuthController controller = controller();
        OidcUser user = oidcUser(Map.of("email", " ", "email_verified", true, "sub", "auth0|test-user"));

        assertThatThrownBy(() -> controller.verifyStepUp("123456", user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
    }

    private AuthController controller() {
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
        InMemoryAuditService auditService = new InMemoryAuditService(100);
        AuthFlowRegistry registry = new AuthFlowRegistry(Map.of(
                "login", new AuthStateEngine(loginFlow, auditService),
                "step-up", new AuthStateEngine(stepUpFlow, auditService)
        ));
        return new AuthController(
                registry,
                new AuthorizationService(List.of("APP_USER")),
                new AuthSessionService(),
                auditService,
                new InMemoryMfaChallengeService(new AuthMfaProperties("123456")),
                new AuthViewMapper()
        );
    }

    private OidcUser oidcUser() {
        return oidcUser(Map.of(
                "email", "user@example.com",
                "email_verified", true,
                "sub", "auth0|test-user"
        ));
    }

    private OidcUser oidcUser(Map<String, Object> claims) {
        Map<String, Object> mutableClaims = new java.util.HashMap<>(claims);
        mutableClaims.putIfAbsent("sub", "auth0|test-user");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(300), mutableClaims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");
    }
}
