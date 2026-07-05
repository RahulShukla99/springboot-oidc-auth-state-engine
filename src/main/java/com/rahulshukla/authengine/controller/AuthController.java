package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.AuthAuditRecord;
import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.service.AuthorizationDecision;
import com.rahulshukla.authengine.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {
    private final AuthStateEngine stateEngine;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final InMemoryAuditService auditService;
    private final AuthFlow authFlow;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "Spring Boot OIDC Authentication State Engine",
                "loginUrl", "/oauth2/authorization/auth0"
        );
    }

    @GetMapping("/auth/success")
    public AuthSessionContext success(@AuthenticationPrincipal OidcUser user) {
        AuthorizationDecision decision = authorizationService.authorize(user);
        String username = user == null ? null : user.getEmail();
        if (username == null || username.isBlank()) {
            return executeNewLoginFlow(user, decision);
        }
        return sessionService.findOrCreateCompletedSession(username, () -> executeNewLoginFlow(user, decision));
    }

    @GetMapping("/auth/session")
    public Map<String, Object> session(Authentication authentication, @AuthenticationPrincipal OidcUser user) {
        String username = user == null ? null : user.getEmail();
        AuthSessionContext context = username == null ? null : sessionService.findByUsername(username).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        response.put("username", username);
        response.put("fullName", user == null ? null : user.getFullName());
        response.put("emailVerified", user != null && !Boolean.FALSE.equals(user.getEmailVerified()));
        response.put("authorities", authentication == null ? List.of() : authentication.getAuthorities());
        response.put("currentState", context == null ? null : context.getCurrentState());
        response.put("finalState", context == null ? null : context.getFinalState());
        return response;
    }

    @GetMapping("/auth/flow")
    public Map<String, Object> flow() {
        return Map.of(
                "flowName", authFlow.name(),
                "initialState", authFlow.initialState().id(),
                "finalStates", authFlow.finalStates().stream().map(state -> state.id()).toList(),
                "states", authFlow.states(),
                "transitions", authFlow.states().stream()
                        .flatMap(state -> state.transitions().stream()
                                .map(transition -> Map.of("fromState", state.id(), "event", transition.event(), "toState", transition.target())))
                        .toList()
        );
    }

    @GetMapping("/auth/audit")
    public List<AuthAuditRecord> audit() {
        return auditService.recentRecords();
    }

    private AuthSessionContext executeNewLoginFlow(OidcUser user, AuthorizationDecision decision) {
        AuthSessionContext context = buildContext(user);
        if (!decision.authorized()) {
            context.setFailureReason(decision.reason());
        }
        return stateEngine.executeLoginFlow(context);
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
}
