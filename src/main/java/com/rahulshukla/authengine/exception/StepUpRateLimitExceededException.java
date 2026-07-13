package com.rahulshukla.authengine.exception;

public class StepUpRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public StepUpRateLimitExceededException(long retryAfterSeconds) {
        super("Step-up rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
