package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.audit.AuthAuditRecord;
import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.exception.AuthStateException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthSessionContext;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import com.rahulshukla.authengine.model.TransitionHistoryEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic authentication workflow executor.
 * <p>
 * The engine advances a session through the configured flow, records each transition,
 * and writes audit events for success and failure outcomes.
 */
@Slf4j
public class AuthStateEngine {
    private final AuthFlow flow;
    private final InMemoryAuditService auditService;
    private final TransitionRuleResolver transitionRuleResolver;
    private final Map<String, AuthState> statesById;

    public AuthStateEngine(AuthFlow flow, InMemoryAuditService auditService) {
        this(flow, auditService, new DefaultTransitionRuleResolver());
    }

    public AuthStateEngine(AuthFlow flow, InMemoryAuditService auditService, TransitionRuleResolver transitionRuleResolver) {
        this.flow = flow;
        this.auditService = auditService;
        this.transitionRuleResolver = transitionRuleResolver;
        this.statesById = flow.states().stream().collect(Collectors.toUnmodifiableMap(AuthState::id, Function.identity()));
    }

    /**
     * Returns the configured workflow definition.
     */
    public AuthFlow flow() {
        return flow;
    }

    /**
     * Returns the initial state for the workflow.
     */
    public AuthState getInitialState() {
        return flow.initialState();
    }

    /**
     * Resolves the next state for a given current state and event.
     */
    public AuthState transition(String currentState, String event) {
        AuthState state = statesById.get(currentState);
        if (state == null) {
            throw new AuthStateException("Current state does not exist: " + currentState);
        }
        AuthTransition transition = state.transitions().stream()
                .filter(candidate -> candidate.event().equals(event))
                .findFirst()
                .orElseThrow(() -> new AuthStateException("Event " + event + " is not allowed from state " + currentState));
        AuthState target = statesById.get(transition.target());
        if (target == null) {
            throw new AuthStateException("Target state does not exist: " + transition.target());
        }
        return target;
    }

    /**
     * Executes the full login workflow for a single session context.
     */
    public AuthSessionContext executeLoginFlow(AuthSessionContext context) {
        log.info("workflow execution started correlationId={} username={} flow={}", context.getCorrelationId(), context.getUsername(), flow.name());
        context.setCurrentState(getInitialState().id());
        if (!context.isEmailVerified() && context.getFailureReason() == null) {
            context.setFailureReason("Email is not verified");
        }
        while (true) {
            AuthState current = statesById.get(context.getCurrentState());
            if (current == null) {
                throw new AuthStateException("Current state does not exist: " + context.getCurrentState());
            }
            if (current.finalState()) {
                break;
            }
            ResolvedTransition transition = resolveTransition(current, context);
            move(context, transition.transition(), transition.rule().outcome());
        }
        log.info("workflow execution completed correlationId={} username={} finalState={}", context.getCorrelationId(), context.getUsername(), context.getFinalState());
        return context;
    }

    private ResolvedTransition resolveTransition(AuthState current, AuthSessionContext context) {
        return current.transitions().stream()
                .map(transition -> new ResolvedTransition(transition, transitionRuleResolver.resolve(flow.name(), transition.event())))
                .filter(candidate -> candidate.rule().matches(context))
                .findFirst()
                .orElseThrow(() -> new AuthStateException("No transition guard matched from state " + current.id()));
    }

    private void move(AuthSessionContext context, AuthTransition transition, String outcome) {
        String from = context.getCurrentState();
        log.debug("workflow transition correlationId={} username={} fromState={} event={}", context.getCorrelationId(), context.getUsername(), from, transition.event());
        AuthState target = transition(from, transition.event());
        context.setCurrentState(target.id());
        if (target.finalState()) {
            context.setFinalState(target.id());
        }
        Instant timestamp = Instant.now();
        context.addTransition(new TransitionHistoryEntry(from, transition.event(), target.id(), timestamp));
        auditService.record(new AuthAuditRecord(context.getCorrelationId(), context.getUsername(), from, transition.event(), target.id(), timestamp, outcome));
    }

    private record ResolvedTransition(AuthTransition transition, TransitionRule rule) {
    }
}
