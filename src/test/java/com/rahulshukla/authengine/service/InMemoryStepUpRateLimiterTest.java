package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.config.StepUpRateLimitProperties;
import com.rahulshukla.authengine.exception.StepUpRateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryStepUpRateLimiterTest {

    @Test
    void shouldAllowOnlyFiveAttemptsWithinRollingWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-13T20:00:00Z"), ZoneId.of("UTC"));
        InMemoryStepUpRateLimiter limiter = new InMemoryStepUpRateLimiter(new StepUpRateLimitProperties(5, Duration.ofSeconds(60)), clock);

        for (int attempt = 1; attempt <= 5; attempt++) {
            limiter.checkAllowed("user@example.com");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("user@example.com"))
                .isInstanceOf(StepUpRateLimitExceededException.class)
                .hasMessage("Step-up rate limit exceeded");
    }

    @Test
    void shouldAllowAttemptsAgainAfterWindowElapses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-13T20:00:00Z"), ZoneId.of("UTC"));
        InMemoryStepUpRateLimiter limiter = new InMemoryStepUpRateLimiter(new StepUpRateLimitProperties(5, Duration.ofSeconds(60)), clock);

        for (int attempt = 1; attempt <= 5; attempt++) {
            limiter.checkAllowed("user@example.com");
        }

        clock.advance(Duration.ofSeconds(61));

        limiter.checkAllowed("user@example.com");
    }

    @Test
    void shouldAllowAgainAfterRejectedAttemptOnceOriginalWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-13T20:00:00Z"), ZoneId.of("UTC"));
        InMemoryStepUpRateLimiter limiter = new InMemoryStepUpRateLimiter(new StepUpRateLimitProperties(1, Duration.ofSeconds(60)), clock);

        limiter.checkAllowed("user@example.com");
        clock.advance(Duration.ofSeconds(59));
        assertThatThrownBy(() -> limiter.checkAllowed("user@example.com"))
                .isInstanceOf(StepUpRateLimitExceededException.class);

        clock.advance(Duration.ofSeconds(2));
        limiter.checkAllowed("user@example.com");
    }

    @Test
    void shouldTrackAttemptsPerUsernameIndependently() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-13T20:00:00Z"), ZoneId.of("UTC"));
        InMemoryStepUpRateLimiter limiter = new InMemoryStepUpRateLimiter(new StepUpRateLimitProperties(5, Duration.ofSeconds(60)), clock);

        for (int attempt = 1; attempt <= 5; attempt++) {
            limiter.checkAllowed("user-one@example.com");
        }

        limiter.checkAllowed("user-two@example.com");

        assertThatThrownBy(() -> limiter.checkAllowed("user-one@example.com"))
                .isInstanceOf(StepUpRateLimitExceededException.class);
    }

    @Test
    void shouldRejectOnlyTheSixthConcurrentAttemptForSameUsername() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-13T20:00:00Z"), ZoneId.of("UTC"));
        InMemoryStepUpRateLimiter limiter = new InMemoryStepUpRateLimiter(new StepUpRateLimitProperties(5, Duration.ofSeconds(60)), clock);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch ready = new CountDownLatch(6);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        limiter.checkAllowed("user@example.com");
                        allowed.incrementAndGet();
                    } catch (StepUpRateLimitExceededException ex) {
                        rejected.incrementAndGet();
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(allowed.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(1);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
