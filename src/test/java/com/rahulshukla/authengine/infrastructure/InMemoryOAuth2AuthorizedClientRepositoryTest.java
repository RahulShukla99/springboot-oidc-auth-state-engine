package com.rahulshukla.authengine.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryOAuth2AuthorizedClientRepositoryTest {

    @Test
    void shouldRefreshExpiredAccessTokenAndPersistUpdatedAuthorizedClient() {
        ClientRegistration registration = clientRegistration();
        ClientRegistrationRepository registrations = new InMemoryClientRegistrationRepository(registration);
        InMemoryOAuth2AuthorizedClientRepository repository = new InMemoryOAuth2AuthorizedClientRepository();
        OAuth2AuthorizedClientManager manager = manager(registrations, repository);

        Authentication principal = UsernamePasswordAuthenticationToken.authenticated("user@example.com", "credentials", List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), principal, request, response);

        OAuth2AuthorizedClient refreshed = manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("auth0")
                .principal(principal)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build());

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.getAccessToken().getTokenValue()).isEqualTo("refreshed-access-token");
        assertThat(refreshed.getRefreshToken().getTokenValue()).isEqualTo("refreshed-refresh-token");
        OAuth2AuthorizedClient stored = repository.loadAuthorizedClient("auth0", principal, request);
        assertThat(stored).isNotNull();
        assertThat(stored.getAccessToken().getTokenValue()).isEqualTo("refreshed-access-token");
    }

    @Test
    void shouldIgnoreBlankOrMissingInputs() {
        ClientRegistration registration = clientRegistration();
        InMemoryOAuth2AuthorizedClientRepository repository = new InMemoryOAuth2AuthorizedClientRepository();
        Authentication blankPrincipal = UsernamePasswordAuthenticationToken.authenticated(" ", "credentials", List.of());
        Authentication nullNamePrincipal = mock(Authentication.class);
        when(nullNamePrincipal.getName()).thenReturn(null);
        Authentication principal = UsernamePasswordAuthenticationToken.authenticated("user@example.com", "credentials", List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);

        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", blankPrincipal, request)).isNull();
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", nullNamePrincipal, request)).isNull();
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", principal, new MockHttpServletRequest())).isNull();
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", principal, null)).isNull();
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", null, request)).isNull();

        repository.saveAuthorizedClient(null, principal, request, new MockHttpServletResponse());
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), null, request, new MockHttpServletResponse());
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), blankPrincipal, request, new MockHttpServletResponse());
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), nullNamePrincipal, request, new MockHttpServletResponse());
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), principal, null, new MockHttpServletResponse());

        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", principal, request)).isNull();

        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), principal, request, new MockHttpServletResponse());
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", principal, request)).isNotNull();

        Authentication secondPrincipal = UsernamePasswordAuthenticationToken.authenticated("other@example.com", "credentials", List.of());
        repository.saveAuthorizedClient(expiredAuthorizedClient(registration), secondPrincipal, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, request, new MockHttpServletResponse());
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", secondPrincipal, request)).isNotNull();

        MockHttpServletRequest otherSessionRequest = new MockHttpServletRequest();
        otherSessionRequest.getSession(true);
        repository.removeAuthorizedClient("auth0", principal, otherSessionRequest, new MockHttpServletResponse());

        repository.removeAuthorizedClient("auth0", null, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, new MockHttpServletRequest(), new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, request, new MockHttpServletResponse());
        assertThat((OAuth2AuthorizedClient) repository.loadAuthorizedClient("auth0", principal, request)).isNull();

        repository.removeAuthorizedClient("auth0", blankPrincipal, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", nullNamePrincipal, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, new MockHttpServletRequest(), new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, null, new MockHttpServletResponse());

        repository.removeAuthorizedClient("auth0", blankPrincipal, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", nullNamePrincipal, request, new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, new MockHttpServletRequest(), new MockHttpServletResponse());
        repository.removeAuthorizedClient("auth0", principal, null, new MockHttpServletResponse());
    }

    private OAuth2AuthorizedClientManager manager(ClientRegistrationRepository registrations,
                                                  InMemoryOAuth2AuthorizedClientRepository repository) {
        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(registrations, repository);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken(refresh -> refresh.accessTokenResponseClient(refreshTokenResponseClient()))
                .build());
        return manager;
    }

    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient() {
        return request -> OAuth2AccessTokenResponse.withToken("refreshed-access-token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(300)
                .refreshToken("refreshed-refresh-token")
                .build();
    }

    private OAuth2AuthorizedClient expiredAuthorizedClient(ClientRegistration registration) {
        Instant now = Instant.now();
        return new OAuth2AuthorizedClient(
                registration,
                "user@example.com",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "expired-access-token", now.minusSeconds(3600), now.minusSeconds(300)),
                new OAuth2RefreshToken("initial-refresh-token", now.minusSeconds(3600))
        );
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
                .userInfoUri("https://example.us.auth0.com/userinfo")
                .userNameAttributeName("sub")
                .clientName("Auth0")
                .build();
    }
}
