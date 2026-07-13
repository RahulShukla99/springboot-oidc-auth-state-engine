package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.config.StepUpRateLimitProperties;
import com.rahulshukla.authengine.exception.StepUpRateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class InMemoryStepUpRateLimiter implements StepUpRateLimiter {
    private final StepUpRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Instant>> attemptsByUsername = new ConcurrentHashMap<>();

    public InMemoryStepUpRateLimiter(StepUpRateLimitProperties properties, Clock stepUpRateLimitClock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(stepUpRateLimitClock, "stepUpRateLimitClock must not be null");
    }

    @Override
    public void checkAllowed(String username) {
        Objects.requireNonNull(username, "username must not be null");
        Instant now = clock.instant();
        attemptsByUsername.compute(username, (key, attempts) -> applyWindow(attempts, now));
    }

    private Deque<Instant> applyWindow(Deque<Instant> attempts, Instant now) {
        Deque<Instant> history = attempts == null ? new ArrayDeque<>() : attempts;
        Instant cutoff = now.minus(properties.window());
        history.removeIf(instant -> instant.isBefore(cutoff));
        if (history.size() >= properties.maxAttempts()) {
            Instant oldestAttempt = history.peekFirst();
            long retryAfterSeconds = Math.max(1L, Duration.between(now, oldestAttempt.plus(properties.window())).toSeconds());
            throw new StepUpRateLimitExceededException(retryAfterSeconds);
        }
        history.addLast(now);
        return history;
    }
}
