package com.rahulshukla.authengine.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Auth0LogoutSuccessHandler implements LogoutSuccessHandler {
    private final String issuerUri;
    private final String clientId;
    private final String postLogoutRedirectUri;

    public Auth0LogoutSuccessHandler(String issuerUri, String clientId, String postLogoutRedirectUri) {
        this.issuerUri = normalize(issuerUri);
        this.clientId = normalize(clientId);
        this.postLogoutRedirectUri = normalize(postLogoutRedirectUri);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", logoutUrl());
    }

    String logoutUrl() {
        if (issuerUri == null || clientId == null) {
            return postLogoutRedirectUri == null ? "/" : postLogoutRedirectUri;
        }
        String logoutBase = issuerUri.endsWith("/") ? issuerUri + "v2/logout" : issuerUri + "/v2/logout";
        return logoutBase + "?client_id="
                + encodeQueryParam(clientId)
                + "&returnTo="
                + encodeQueryParam(postLogoutRedirectUri == null ? "/" : postLogoutRedirectUri);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String encodeQueryParam(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }
}
