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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private static final String OIDC_POST_LOGIN_FLOW = "oidc-post-login-auth-flow";
    private static final String STEP_UP_MFA_FLOW = "step-up-mfa-flow";

    private final AuthFlowRegistry flowRegistry;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final InMemoryAuditService auditService;
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
     * Returns the default workflow definition as JSON.
     */
    @GetMapping(value = "/auth/flow", produces = MediaType.APPLICATION_JSON_VALUE)
    public FlowResponse flow() {
        return flow(DEFAULT_FLOW);
    }

    /**
     * Returns the default workflow definition as browser-friendly HTML.
     */
    @GetMapping(value = "/auth/flow/view", produces = MediaType.TEXT_HTML_VALUE)
    public String flowView() {
        return flowView(DEFAULT_FLOW);
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
     * Returns the requested workflow definition as browser-friendly HTML.
     */
    @GetMapping(value = "/auth/flow/{flowName}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String flowView(@PathVariable String flowName) {
        AuthFlow authFlow = flowRegistry.getRequiredEngine(flowName).flow();
        log.debug("flow html view requested flow={} stateCount={}", flowName, authFlow.states().size());
        return renderFlowHtml(viewMapper.toFlowResponse(authFlow));
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

    private String renderFlowHtml(FlowResponse flow) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    :root { color: #101828; font-family: Arial, sans-serif; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      background: #ffffff;
                      color: #101828;
                    }
                    .diagram-shell {
                      min-height: 100vh;
                      display: flex;
                      align-items: flex-start;
                      justify-content: center;
                      padding: 12px;
                    }
                    .diagram-frame {
                      margin: 0;
                      width: min(100%%, 920px);
                    }
                    .diagram-frame svg {
                      display: block;
                      width: 100%%;
                      height: auto;
                    }
                    .sr-only {
                      position: absolute;
                      width: 1px;
                      height: 1px;
                      padding: 0;
                      margin: -1px;
                      overflow: hidden;
                      clip: rect(0, 0, 0, 0);
                      white-space: nowrap;
                      border: 0;
                    }
                  </style>
                </head>
                <body>
                  <main class="diagram-shell">
                    <figure class="diagram-frame" data-renderer="%s">
                      <figcaption class="sr-only">%s</figcaption>
                      %s
                    </figure>
                  </main>
                </body>
                </html>
                """.formatted(
                flow.flowName(),
                rendererName(flow),
                accessibleSummary(flow),
                renderDiagramMarkup(flow)
        );
    }

    private String renderDiagramMarkup(FlowResponse flow) {
        String graphvizSvgResource = graphvizSvgResource(flow.flowName());
        if (graphvizSvgResource != null) {
            readClasspathResource(graphvizDotResource(flow.flowName()));
            return inlineSvgMarkup(readClasspathResource(graphvizSvgResource));
        }
        return "<pre>" + escapeHtml(toMermaid(flow)) + "</pre>";
    }

    private String rendererName(FlowResponse flow) {
        return graphvizSvgResource(flow.flowName()) != null ? "graphviz" : "text";
    }

    private String accessibleSummary(FlowResponse flow) {
        return flow.flowName()
                + " diagram. Initial state " + flow.initialState()
                + ". Final states " + String.join(", ", flow.finalStates()) + ".";
    }

    private String graphvizSvgResource(String flowName) {
        return switch (flowName) {
            case OIDC_POST_LOGIN_FLOW -> "diagrams/oidc-post-login-auth-flow.svg";
            case STEP_UP_MFA_FLOW -> "diagrams/step-up-mfa-flow.svg";
            default -> null;
        };
    }

    private String graphvizDotResource(String flowName) {
        return switch (flowName) {
            case OIDC_POST_LOGIN_FLOW -> "diagrams/oidc-post-login-auth-flow.dot";
            case STEP_UP_MFA_FLOW -> "diagrams/step-up-mfa-flow.dot";
            default -> throw new IllegalArgumentException("No Graphviz source configured for flow " + flowName);
        };
    }

    private String toMermaid(FlowResponse flow) {
        StringBuilder diagram = new StringBuilder("stateDiagram-v2\n");
        diagram.append("    [*] --> ").append(flow.initialState()).append('\n');
        flow.transitions().forEach(transition -> diagram.append("    ")
                .append(transition.fromState())
                .append(" --> ")
                .append(transition.toState())
                .append(": ")
                .append(transition.event())
                .append('\n'));
        flow.finalStates().forEach(state -> diagram.append("    ").append(state).append(" --> [*]\n"));
        return diagram.toString();
    }

    private String inlineSvgMarkup(String svg) {
        String normalizedSvg = svg.replaceFirst("^\\s*<\\?xml[^>]*>\\s*", "");
        return normalizedSvg.replaceFirst("<svg\\b", "<svg class=\"graphviz-diagram\"");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String readClasspathResource(String resourcePath) {
        try (var inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render flow diagram from resource " + resourcePath, exception);
        }
    }

    private String username(OidcUser user) {
        return Optional.ofNullable(user).map(OidcUser::getEmail).orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
