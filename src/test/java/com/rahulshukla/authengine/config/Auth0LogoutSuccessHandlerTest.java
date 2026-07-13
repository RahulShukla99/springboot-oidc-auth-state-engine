package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Auth0LogoutSuccessHandlerTest {

    @Test
    void shouldRedirectToAuth0FederatedLogoutEndpoint() throws Exception {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(
                "https://example.us.auth0.com/",
                "client-123",
                "http://localhost:8080/"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);

        handler.onLogoutSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://example.us.auth0.com/v2/logout?client_id=client-123&returnTo=http://localhost:8080/");
    }

    @Test
    void shouldFallbackToLocalRedirectWhenAuth0DetailsAreMissing() throws Exception {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(null, null, "http://localhost:8080/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(new MockHttpServletRequest(), response, mock(Authentication.class));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080/");
    }

    @Test
    void shouldFallbackToRootWhenPostLogoutRedirectIsMissing() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(null, null, null);

        assertThat(handler.logoutUrl()).isEqualTo("/");
    }

    @Test
    void shouldAppendLogoutPathWhenIssuerUriDoesNotEndWithSlash() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(
                "https://example.us.auth0.com",
                "client-123",
                "http://localhost:8080/"
        );

        assertThat(handler.logoutUrl())
                .isEqualTo("https://example.us.auth0.com/v2/logout?client_id=client-123&returnTo=http://localhost:8080/");
    }

    @Test
    void shouldNormalizeBlankIssuerAndClientValues() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(" ", " ", " ");

        assertThat(handler.logoutUrl()).isEqualTo("/");
    }

    @Test
    void shouldFallbackToLocalRedirectWhenIssuerIsMissingButClientExists() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(null, "client-123", "http://localhost:8080/");

        assertThat(handler.logoutUrl()).isEqualTo("http://localhost:8080/");
    }

    @Test
    void shouldFallbackToLocalRedirectWhenClientIsMissingButIssuerExists() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler("https://example.us.auth0.com/", null, "http://localhost:8080/");

        assertThat(handler.logoutUrl()).isEqualTo("http://localhost:8080/");
    }

    @Test
    void shouldAppendLogoutPathWhenIssuerUriEndsWithSlash() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(
                "https://example.us.auth0.com/",
                "client-123",
                "http://localhost:8080/callback"
        );

        assertThat(handler.logoutUrl())
                .isEqualTo("https://example.us.auth0.com/v2/logout?client_id=client-123&returnTo=http://localhost:8080/callback");
    }

    @Test
    void shouldFallbackToRootReturnToWhenRedirectIsMissingButAuth0DetailsExist() {
        Auth0LogoutSuccessHandler handler = new Auth0LogoutSuccessHandler(
                "https://example.us.auth0.com/",
                "client-123",
                null
        );

        assertThat(handler.logoutUrl())
                .isEqualTo("https://example.us.auth0.com/v2/logout?client_id=client-123&returnTo=/");
    }
}
