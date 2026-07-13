package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthJwtDecoderConfigTest {

    @Test
    void shouldAcceptTokenJustInsideAllowedClockSkew() {
        JwtTimestampValidator validator = AuthJwtDecoderConfig.clockSkewValidator(Duration.ofSeconds(60));
        Instant now = Instant.parse("2026-07-13T20:00:00Z");
        validator.setClock(Clock.fixed(now, ZoneOffset.UTC));
        Jwt inside = jwt(now, now.plusSeconds(59), now.plusSeconds(300));

        var result = validator.validate(inside);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldRejectTokenJustOutsideAllowedClockSkew() {
        JwtTimestampValidator validator = AuthJwtDecoderConfig.clockSkewValidator(Duration.ofSeconds(60));
        Instant now = Instant.parse("2026-07-13T20:00:00Z");
        validator.setClock(Clock.fixed(now, ZoneOffset.UTC));
        Jwt outside = jwt(now, now.plusSeconds(61), now.plusSeconds(300));
        Jwt inside = jwt(now, now.plusSeconds(59), now.plusSeconds(300));

        assertThat(validator.validate(inside).hasErrors()).isFalse();
        assertThat(validator.validate(outside).hasErrors()).isTrue();
    }

    @Test
    void shouldCreateDecoderWithConfiguredClockSkew() {
        AuthJwtDecoderConfig config = new AuthJwtDecoderConfig();
        var factory = config.oidcIdTokenDecoderFactory(new AuthJwtProperties(60));

        assertThat(factory.createDecoder(clientRegistration())).isNotNull();
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("auth0")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/auth0")
                .scope("openid", "profile", "email")
                .authorizationUri("https://example.us.auth0.com/authorize")
                .tokenUri("https://example.us.auth0.com/oauth/token")
                .issuerUri("https://example.us.auth0.com/")
                .jwkSetUri("https://example.us.auth0.com/.well-known/jwks.json")
                .userInfoUri("https://example.us.auth0.com/userinfo")
                .userNameAttributeName("sub")
                .clientName("Auth0")
                .build();
    }

    private Jwt jwt(Instant issuedAt, Instant notBefore, Instant expiresAt) {
        return new Jwt(
                "token",
                issuedAt,
                expiresAt,
                Map.of("alg", "none"),
                Map.of("sub", "auth0|test", "nbf", notBefore, "iat", issuedAt, "exp", expiresAt)
        );
    }
}
