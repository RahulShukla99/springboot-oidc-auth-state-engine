package com.rahulshukla.authengine.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServiceTest {

    @Test
    void shouldAllowVerifiedUser() {
        AuthorizationService service = new AuthorizationService(List.of("APP_USER", "APP_ADMIN"));
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", true, "groups", List.of("APP_USER")));

        AuthorizationDecision decision = service.authorize(user);

        assertThat(decision.authorized()).isTrue();
    }

    @Test
    void shouldRejectMissingEmail() {
        AuthorizationService service = new AuthorizationService(List.of("APP_USER"));
        OidcUser user = oidcUser(Map.of("email_verified", true));

        AuthorizationDecision decision = service.authorize(user);

        assertThat(decision.authorized()).isFalse();
        assertThat(decision.reason()).isEqualTo("OIDC user email is missing");
    }

    @Test
    void shouldAllowWhenGroupsAreMissingAndEmailIsVerified() {
        AuthorizationService service = new AuthorizationService(List.of("APP_USER"));
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", true));

        AuthorizationDecision decision = service.authorize(user);

        assertThat(decision.authorized()).isTrue();
        assertThat(decision.reason()).isEqualTo("Authorized because email is verified and group claims are not present");
    }

    @Test
    void shouldRejectUnverifiedEmail() {
        AuthorizationService service = new AuthorizationService(List.of("APP_USER"));
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", false));

        AuthorizationDecision decision = service.authorize(user);

        assertThat(decision.authorized()).isFalse();
        assertThat(decision.reason()).isEqualTo("OIDC user email is not verified");
    }

    @Test
    void shouldRejectUserWithoutAllowedGroupWhenGroupsArePresent() {
        AuthorizationService service = new AuthorizationService(List.of("APP_USER"));
        OidcUser user = oidcUser(Map.of("email", "user@example.com", "email_verified", true, "groups", List.of("OTHER_GROUP")));

        AuthorizationDecision decision = service.authorize(user);

        assertThat(decision.authorized()).isFalse();
        assertThat(decision.reason()).isEqualTo("OIDC user does not belong to an allowed group");
    }

    private OidcUser oidcUser(Map<String, Object> claims) {
        java.util.HashMap<String, Object> mutableClaims = new java.util.HashMap<>(claims);
        mutableClaims.putIfAbsent("sub", "auth0|test-user");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(300), mutableClaims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");
    }
}
