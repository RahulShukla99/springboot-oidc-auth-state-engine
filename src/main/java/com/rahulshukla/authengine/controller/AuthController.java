package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.audit.AuthAuditRecord;
import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.service.AuthSessionService;
import com.rahulshukla.authengine.exception.StepUpRateLimitExceededException;
import com.rahulshukla.authengine.service.AuthorizationDecision;
import com.rahulshukla.authengine.service.AuthorizationService;
import com.rahulshukla.authengine.service.MfaChallengeService;
import com.rahulshukla.authengine.service.StepUpRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * HTTP entry point for the authentication workflow.
 * <p>
 * The controller exposes JSON endpoints for the active workflow and an HTML view for
 * browser-based flow inspection.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private static final String DEFAULT_FLOW = "login";
    private static final String STEP_UP_FLOW = "step-up";

    private final AuthFlowRegistry flowRegistry;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final InMemoryAuditService auditService;
    private final StepUpRateLimiter stepUpRateLimiter;
    private final MfaChallengeService mfaChallengeService;
    private final AuthViewMapper viewMapper;

    /**
     * Returns the landing payload with the default login entry point.
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public HomeResponse home() {
        log.debug("home endpoint requested");
        return new HomeResponse("Spring Boot OIDC Authentication State Engine", "/oauth2/authorization/auth0");
    }

    /**
     * Executes the default login workflow for the authenticated user.
     */
    @GetMapping(value = "/auth/success", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthSessionContext success(@AuthenticationPrincipal OidcUser user) {
        return success(DEFAULT_FLOW, user);
    }

    /**
     * Executes the requested workflow and keeps the result idempotent per user.
     */
    @GetMapping(value = "/auth/success/{flowName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthSessionContext success(@PathVariable String flowName, @AuthenticationPrincipal OidcUser user) {
        AuthorizationDecision decision = authorizationService.authorize(user);
        String username = username(user);
        log.info("auth success requested flow={} username={}", flowName, username);
        return isBlank(username)
                ? executeNewLoginFlow(flowName, user, decision)
                : sessionService.findOrCreateCompletedSession(flowName, username, () -> executeNewLoginFlow(flowName, user, decision));
    }

    /**
     * Returns the current session snapshot for the default workflow.
     */
    @GetMapping(value = "/auth/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public SessionResponse session(Authentication authentication, @AuthenticationPrincipal OidcUser user) {
        return session(DEFAULT_FLOW, authentication, user);
    }

    /**
     * Returns the current session snapshot for the requested workflow.
     */
    @GetMapping(value = "/auth/session/{flowName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SessionResponse session(@PathVariable String flowName, Authentication authentication, @AuthenticationPrincipal OidcUser user) {
        String username = username(user);
        AuthSessionContext context = username == null ? null : sessionService.findByFlowAndUsername(flowName, username).orElse(null);
        log.debug("session snapshot requested flow={} username={} found={}", flowName, username, context != null);
        return viewMapper.toSessionResponse(authentication, user, context);
    }

    /**
     * Verifies the step-up MFA challenge and executes the step-up workflow.
     */
    @PostMapping(value = "/auth/step-up/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthSessionContext verifyStepUp(@RequestParam String code, @AuthenticationPrincipal OidcUser user) {
        if (isBlank(code)) {
            throw new IllegalArgumentException("code must not be blank");
        }
        AuthorizationDecision decision = authorizationService.authorize(user);
        String username = username(user);
        log.info("auth step-up verify requested username={}", username);
        return isBlank(username)
                ? executeNewStepUpFlow(user, code, decision)
                : sessionService.findOrCreateCompletedSession(STEP_UP_FLOW, username, () -> executeNewStepUpFlow(user, code, decision));
    }

    /**
     * Returns the default workflow definition as JSON.
     */
    @GetMapping(value = "/auth/flow", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowResponse flow() {
        return flow(DEFAULT_FLOW);
    }

    /**
     * Returns the requested workflow definition as JSON.
     */
    @GetMapping(value = "/auth/flow/{flowName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowResponse flow(@PathVariable String flowName) {
        AuthFlow authFlow = flowRegistry.getRequiredEngine(flowName).flow();
        log.debug("flow definition requested flow={} stateCount={}", flowName, authFlow.states().size());
        return viewMapper.toFlowResponse(authFlow);
    }

    /**
     * Returns the recent audit trail for the active JVM.
     */
    @GetMapping(value = "/auth/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuthAuditRecord> audit() {
        log.debug("audit trail requested");
        return auditService.recentRecords();
    }

    private AuthSessionContext executeNewLoginFlow(String flowName, OidcUser user, AuthorizationDecision decision) {
        AuthSessionContext context = buildContext(user);
        if (!decision.authorized()) {
            context.setFailureReason(decision.reason());
        }
        log.info("executing workflow flow={} username={} authorized={}", flowName, context.getUsername(), decision.authorized());
        AuthSessionContext result = flowRegistry.getRequiredEngine(flowName).executeLoginFlow(context);
        log.info("workflow completed flow={} username={} finalState={}", flowName, result.getUsername(), result.getFinalState());
        return result;
    }

    private AuthSessionContext executeNewStepUpFlow(OidcUser user, String code, AuthorizationDecision decision) {
        AuthSessionContext context = buildContext(user);
        if (!decision.authorized()) {
            context.setFailureReason(decision.reason());
        }
        String username = context.getUsername();
        if (!isBlank(username)) {
            try {
                stepUpRateLimiter.checkAllowed(username);
            } catch (StepUpRateLimitExceededException ex) {
                auditService.record(new AuthAuditRecord(
                        context.getCorrelationId(),
                        username,
                        "REQUIRE_MFA",
                        "RATE_LIMITED",
                        "REQUIRE_MFA",
                        Instant.now(),
                        "RATE_LIMITED"
                ));
                throw ex;
            }
        }
        mfaChallengeService.issueChallenge(username);
        context.setMfaPassed(mfaChallengeService.verifyChallenge(username, code));
        if (!context.isMfaPassed() && context.getFailureReason() == null) {
            context.setFailureReason("MFA challenge failed");
        }
        log.info("executing workflow flow={} username={} authorized={} mfaPassed={}", STEP_UP_FLOW, username, decision.authorized(), context.isMfaPassed());
        AuthSessionContext result = flowRegistry.getRequiredEngine(STEP_UP_FLOW).executeLoginFlow(context);
        log.info("workflow completed flow={} username={} finalState={}", STEP_UP_FLOW, result.getUsername(), result.getFinalState());
        return result;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record HomeResponse(String message, String loginUrl) {
    }

    public record SessionResponse(boolean authenticated,
                                  String username,
                                  String fullName,
                                  boolean emailVerified,
                                  Collection<?> authorities,
                                  String currentState,
                                  String finalState) {
    }

    public record FlowResponse(String flowName,
                               String initialState,
                               List<String> finalStates,
                               List<AuthState> states,
                               List<TransitionView> transitions) {
    }

    public record TransitionView(String fromState, String event, String toState) {
    }
}
