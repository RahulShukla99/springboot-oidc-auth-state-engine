package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthViewMapper {

    public AuthController.FlowResponse toFlowResponse(AuthFlow authFlow) {
        if (authFlow == null) {
            return null;
        }
        return new AuthController.FlowResponse(
                authFlow.name(),
                authFlow.initialState().id(),
                authFlow.finalStates().stream().map(AuthState::id).toList(),
                new ArrayList<>(authFlow.states()),
                toTransitions(authFlow)
        );
    }

    public AuthController.SessionResponse toSessionResponse(Authentication authentication, OidcUser user, AuthSessionContext context) {
        return new AuthController.SessionResponse(
                authentication != null && authentication.isAuthenticated(),
                context != null && context.getUsername() != null ? context.getUsername() : username(user),
                context != null && context.getFullName() != null ? context.getFullName() : fullName(user),
                context != null ? context.isEmailVerified() : emailVerified(user),
                authentication == null ? java.util.List.of() : authentication.getAuthorities(),
                context == null ? null : context.getCurrentState(),
                context == null ? null : context.getFinalState()
        );
    }

    public List<AuthController.TransitionView> toTransitions(AuthFlow authFlow) {
        return authFlow.states().stream()
                .flatMap(state -> state.transitions().stream()
                        .map(transition -> new AuthController.TransitionView(state.id(), transition.event(), transition.target())))
                .toList();
    }

    public String username(OidcUser user) {
        return user == null ? null : user.getEmail();
    }

    public String fullName(OidcUser user) {
        return user == null ? null : user.getFullName();
    }

    public boolean emailVerified(OidcUser user) {
        return user != null && !Boolean.FALSE.equals(user.getEmailVerified());
    }
}
