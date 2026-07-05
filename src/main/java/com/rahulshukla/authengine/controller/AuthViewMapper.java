package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthViewMapper {

    @Mapping(target = "flowName", source = "name")
    @Mapping(target = "initialState", expression = "java(authFlow.initialState().id())")
    @Mapping(target = "finalStates", expression = "java(authFlow.finalStates().stream().map(AuthState::id).toList())")
    @Mapping(target = "transitions", expression = "java(toTransitions(authFlow))")
    AuthController.FlowResponse toFlowResponse(AuthFlow authFlow);

    default AuthController.SessionResponse toSessionResponse(Authentication authentication, OidcUser user, AuthSessionContext context) {
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

    default List<AuthController.TransitionView> toTransitions(AuthFlow authFlow) {
        return authFlow.states().stream()
                .flatMap(state -> state.transitions().stream()
                        .map(transition -> new AuthController.TransitionView(state.id(), transition.event(), transition.target())))
                .toList();
    }

    default String username(OidcUser user) {
        return user == null ? null : user.getEmail();
    }

    default String fullName(OidcUser user) {
        return user == null ? null : user.getFullName();
    }

    default boolean emailVerified(OidcUser user) {
        return user != null && !Boolean.FALSE.equals(user.getEmailVerified());
    }
}
