package com.rahulshukla.authengine.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, OAuth2AuthorizedClient>> clientsBySessionId = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String registrationId, Authentication principal, HttpServletRequest request) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null || principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return null;
        }
        return (T) clientsBySession(session.getId()).get(clientKey(registrationId, principal.getName()));
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        if (authorizedClient == null || request == null || principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return;
        }
        HttpSession session = request.getSession(true);
        clientsBySession(session.getId()).put(clientKey(authorizedClient.getClientRegistration().getRegistrationId(), principal.getName()), authorizedClient);
    }

    @Override
    public void removeAuthorizedClient(String registrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null || principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return;
        }
        ConcurrentHashMap<String, OAuth2AuthorizedClient> clients = clientsBySessionId.get(session.getId());
        if (clients == null) {
            return;
        }
        clients.remove(clientKey(registrationId, principal.getName()));
        if (clients.isEmpty()) {
            clientsBySessionId.remove(session.getId());
        }
    }

    private ConcurrentHashMap<String, OAuth2AuthorizedClient> clientsBySession(String sessionId) {
        return clientsBySessionId.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>());
    }

    private String clientKey(String registrationId, String principalName) {
        return registrationId + ":" + principalName;
    }
}
