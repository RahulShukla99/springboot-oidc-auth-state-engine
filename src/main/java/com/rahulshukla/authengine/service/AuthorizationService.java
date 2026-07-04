package com.rahulshukla.authengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class AuthorizationService {
    private final List<String> allowedGroups;

    public AuthorizationService(@Value("${auth.allowed-groups:APP_USER,APP_ADMIN}") List<String> allowedGroups) {
        this.allowedGroups = allowedGroups.stream().map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    public AuthorizationDecision authorize(OidcUser user) {
        if (user == null) {
            return AuthorizationDecision.deny("OIDC user is not authenticated");
        }
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return AuthorizationDecision.deny("OIDC user email is missing");
        }
        Boolean emailVerified = user.getEmailVerified();
        if (Boolean.FALSE.equals(emailVerified)) {
            return AuthorizationDecision.deny("OIDC user email is not verified");
        }
        List<String> groups = extractGroups(user);
        if (groups.isEmpty()) {
            return AuthorizationDecision.allow("Authorized because email is verified and group claims are not present");
        }
        boolean allowed = groups.stream().anyMatch(allowedGroups::contains);
        return allowed
                ? AuthorizationDecision.allow("Authorized by group claim")
                : AuthorizationDecision.deny("OIDC user does not belong to an allowed group");
    }

    public List<String> extractGroups(OidcUser user) {
        return List.of("groups", "roles", "permissions").stream()
                .map(user::getClaim)
                .filter(Objects::nonNull)
                .flatMap(value -> toStringList(value).stream())
                .distinct()
                .toList();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }
}
