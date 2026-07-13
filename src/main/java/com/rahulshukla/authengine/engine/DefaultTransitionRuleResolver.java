package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.model.AuthSessionContext;

/**
 * Resolves business guards for the configured auth flows.
 */
class DefaultTransitionRuleResolver implements TransitionRuleResolver {
    @Override
    public TransitionRule resolve(String flowName, String event) {
        return switch (event) {
            case "LOGIN_REQUESTED", "OIDC_CALLBACK_RECEIVED", "PROFILE_LOADED" -> TransitionRule.success(context -> true);
            case "TOKEN_VALID" -> TransitionRule.success(AuthSessionContext::isEmailVerified);
            case "TOKEN_INVALID" -> TransitionRule.failure(context -> !context.isEmailVerified());
            case "USER_AUTHORIZED" -> TransitionRule.success(context -> isBlank(context.getFailureReason()));
            case "USER_NOT_AUTHORIZED" -> TransitionRule.failure(context -> !isBlank(context.getFailureReason()));
            case "MFA_PASSED" -> TransitionRule.success(AuthSessionContext::isMfaPassed);
            case "MFA_FAILED" -> TransitionRule.failure(context -> !context.isMfaPassed());
            default -> defaultRule();
        };
    }

    private TransitionRule defaultRule() {
        return TransitionRule.success(context -> true);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
