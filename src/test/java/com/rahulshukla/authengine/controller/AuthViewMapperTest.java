package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthViewMapperTest {
    private final AuthViewMapper mapper = Mappers.getMapper(AuthViewMapper.class);

    @Test
    void shouldMapFlowResponseFromAuthFlow() {
        AuthFlow flow = new AuthFlow("step-up-mfa-flow", List.of(
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

        AuthController.FlowResponse response = mapper.toFlowResponse(flow);

        assertThat(response.flowName()).isEqualTo("step-up-mfa-flow");
        assertThat(response.initialState()).isEqualTo("START");
        assertThat(response.finalStates()).containsExactly("STEP_UP_SUCCESS", "AUTH_FAILED");
        assertThat(response.transitions()).hasSize(7);
        assertThat(response.transitions().getFirst())
                .extracting("fromState", "event", "toState")
                .containsExactly("START", "LOGIN_REQUESTED", "REDIRECT_TO_IDP");
    }

    @Test
    void shouldReturnNullWhenAuthFlowIsMissing() {
        assertThat(mapper.toFlowResponse(null)).isNull();
    }

    @Test
    void shouldMapSessionResponseFromAuthenticationAndContext() {
        AuthSessionContext context = new AuthSessionContext("corr-1");
        context.setCurrentState("AUTHORIZE_USER");
        context.setFinalState("AUTH_SUCCESS");
        context.setUsername("user@example.com");
        context.setFullName("User Example");
        context.setEmailVerified(true);

        OidcUser user = oidcUser(Map.of(
                "email", "user@example.com",
                "email_verified", true,
                "sub", "auth0|123"
        ));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setAuthenticated(true);

        AuthController.SessionResponse response = mapper.toSessionResponse(authentication, user, context);

        assertThat(response.authenticated()).isTrue();
        assertThat(response.username()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isEqualTo("User Example");
        assertThat(response.emailVerified()).isTrue();
        assertThat(response.authorities()).extracting(authority -> ((GrantedAuthority) authority).getAuthority()).containsExactly("ROLE_USER");
        assertThat(response.currentState()).isEqualTo("AUTHORIZE_USER");
        assertThat(response.finalState()).isEqualTo("AUTH_SUCCESS");
    }

    @Test
    void shouldPreferOidcValuesWhenContextIsMissing() {
        OidcUser user = oidcUser(Map.of(
                "email", "user@example.com",
                "email_verified", false,
                "sub", "auth0|123"
        ));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials");
        authentication.setAuthenticated(false);

        AuthController.SessionResponse response = mapper.toSessionResponse(authentication, user, null);

        assertThat(response.authenticated()).isFalse();
        assertThat(response.username()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isNull();
        assertThat(response.emailVerified()).isFalse();
        assertThat(response.authorities()).isEmpty();
        assertThat(response.currentState()).isNull();
        assertThat(response.finalState()).isNull();
    }

    @Test
    void shouldReturnNullIdentityWhenAuthenticationAndUserAreMissing() {
        AuthController.SessionResponse response = mapper.toSessionResponse(null, null, null);

        assertThat(response.authenticated()).isFalse();
        assertThat(response.username()).isNull();
        assertThat(response.fullName()).isNull();
        assertThat(response.emailVerified()).isFalse();
        assertThat(response.authorities()).isEmpty();
    }

    @Test
    void shouldFavorContextValuesAndFallbackToPrincipalValuesWhenPartialContextExists() {
        AuthSessionContext context = new AuthSessionContext("corr-2");
        context.setUsername(null);
        context.setFullName(null);
        context.setEmailVerified(false);
        OidcUser user = oidcUser(Map.of(
                "email", "user@example.com",
                "name", "User Example",
                "email_verified", true,
                "sub", "auth0|456"
        ));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials", List.of());
        authentication.setAuthenticated(true);

        AuthController.SessionResponse response = mapper.toSessionResponse(authentication, user, context);

        assertThat(response.username()).isEqualTo("user@example.com");
        assertThat(response.fullName()).isEqualTo("User Example");
        assertThat(response.emailVerified()).isFalse();
    }

    private OidcUser oidcUser(Map<String, Object> claims) {
        Map<String, Object> mutableClaims = new java.util.HashMap<>(claims);
        mutableClaims.putIfAbsent("sub", "auth0|test-user");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(300), mutableClaims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");
    }
}
