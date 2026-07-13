package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.model.AuthSessionContext;

import java.util.function.Predicate;

public record TransitionRule(Predicate<AuthSessionContext> guard, String outcome) {
    public TransitionRule {
        guard = guard == null ? context -> true : guard;
        outcome = outcome == null || outcome.isBlank() ? "SUCCESS" : outcome;
    }

    public boolean matches(AuthSessionContext context) {
        return guard.test(context);
    }

    public static TransitionRule success(Predicate<AuthSessionContext> guard) {
        return new TransitionRule(guard, "SUCCESS");
    }

    public static TransitionRule failure(Predicate<AuthSessionContext> guard) {
        return new TransitionRule(guard, "FAILURE");
    }
}
