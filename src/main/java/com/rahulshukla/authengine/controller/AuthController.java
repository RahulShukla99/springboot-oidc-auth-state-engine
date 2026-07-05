package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.AuthAuditRecord;
import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.service.AuthorizationDecision;
import com.rahulshukla.authengine.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * HTTP entry point for the authentication workflow.
 * <p>
 * The controller exposes the public flow inspection endpoint and the authenticated
 * post-login endpoints that assemble the session, execute the state engine, and return
 * JSON for the UI.
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {
    private static final String DEFAULT_FLOW = "login";

    private final AuthFlowRegistry flowRegistry;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final InMemoryAuditService auditService;

    @GetMapping("/")
    public HomeResponse home() {
        return new HomeResponse("Spring Boot OIDC Authentication State Engine", "/oauth2/authorization/auth0");
    }

    @GetMapping("/auth/success")
    public AuthSessionContext success(@AuthenticationPrincipal OidcUser user) {
        return success(DEFAULT_FLOW, user);
    }

    @GetMapping("/auth/success/{flowName}")
    public AuthSessionContext success(@PathVariable String flowName, @AuthenticationPrincipal OidcUser user) {
        AuthorizationDecision decision = authorizationService.authorize(user);
        String username = username(user);
        if (username == null || username.isBlank()) {
            return executeNewLoginFlow(flowName, user, decision);
        }
        return sessionService.findOrCreateCompletedSession(flowName, username, () -> executeNewLoginFlow(flowName, user, decision));
    }

    @GetMapping("/auth/session")
    public SessionResponse session(Authentication authentication, @AuthenticationPrincipal OidcUser user) {
        return session(DEFAULT_FLOW, authentication, user);
    }

    @GetMapping("/auth/session/{flowName}")
    public SessionResponse session(@PathVariable String flowName, Authentication authentication, @AuthenticationPrincipal OidcUser user) {
        String username = username(user);
        AuthSessionContext context = username == null ? null : sessionService.findByFlowAndUsername(flowName, username).orElse(null);
        return new SessionResponse(
                authentication != null && authentication.isAuthenticated(),
                username,
                user == null ? null : user.getFullName(),
                user != null && !Boolean.FALSE.equals(user.getEmailVerified()),
                authentication == null ? List.of() : authentication.getAuthorities(),
                context == null ? null : context.getCurrentState(),
                context == null ? null : context.getFinalState()
        );
    }

    @GetMapping("/auth/flow")
    public FlowResponse flow() {
        return flow(DEFAULT_FLOW);
    }

    @GetMapping("/auth/flow/{flowName}")
    public FlowResponse flow(@PathVariable String flowName) {
        AuthFlow authFlow = flowRegistry.getRequiredEngine(flowName).flow();
        return new FlowResponse(
                authFlow.name(),
                authFlow.initialState().id(),
                authFlow.finalStates().stream().map(AuthState::id).toList(),
                authFlow.states(),
                authFlow.states().stream()
                        .flatMap(state -> state.transitions().stream()
                                .map(transition -> new TransitionView(state.id(), transition.event(), transition.target())))
                        .toList()
        );
    }

    @GetMapping("/auth/audit")
    public List<AuthAuditRecord> audit() {
        return auditService.recentRecords();
    }

    private AuthSessionContext executeNewLoginFlow(String flowName, OidcUser user, AuthorizationDecision decision) {
        AuthSessionContext context = buildContext(user);
        if (!decision.authorized()) {
            context.setFailureReason(decision.reason());
        }
        return flowRegistry.getRequiredEngine(flowName).executeLoginFlow(context);
    }

    private AuthSessionContext buildContext(OidcUser user) {
        AuthSessionContext context = new AuthSessionContext();
        if (user != null) {
            context.setUsername(user.getEmail());
            context.setFullName(user.getFullName());
            context.setEmailVerified(!Boolean.FALSE.equals(user.getEmailVerified()));
            context.setGroupsOrRoles(authorizationService.extractGroups(user));
        }
        return context;
    }

    private String username(OidcUser user) {
        return Optional.ofNullable(user).map(OidcUser::getEmail).orElse(null);
    }

    record HomeResponse(String message, String loginUrl) {
    }

    record SessionResponse(boolean authenticated,
                           String username,
                           String fullName,
                           boolean emailVerified,
                           Collection<?> authorities,
                           String currentState,
                           String finalState) {
    }

    record FlowResponse(String flowName,
                        String initialState,
                        List<String> finalStates,
                        List<AuthState> states,
                        List<TransitionView> transitions) {
    }

    record TransitionView(String fromState, String event, String toState) {
    }
}
