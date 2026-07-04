package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.model.AuthSessionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionServiceTest {

    @Test
    void shouldCreateOnlyOneSessionForSameUserWhenRequestsAreConcurrent() throws Exception {
        AuthSessionService service = new AuthSessionService();
        AtomicInteger createCount = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        Callable<AuthSessionContext> task = () -> {
            start.await();
            return service.findOrCreateCompletedSession("user@example.com", () -> {
                createCount.incrementAndGet();
                AuthSessionContext context = new AuthSessionContext("corr-" + createCount.get());
                context.setUsername("user@example.com");
                context.setFinalState("AUTH_SUCCESS");
                return context;
            });
        };

        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Future<AuthSessionContext>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(task));
            }
            start.countDown();

            List<AuthSessionContext> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get());
            }

            assertThat(createCount.get()).isEqualTo(1);
            assertThat(results).extracting(AuthSessionContext::getCorrelationId).containsOnly("corr-1");
        }
    }

    @Test
    void shouldRejectBlankUsernameForIdempotentSessionCreation() {
        AuthSessionService service = new AuthSessionService();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.findOrCreateCompletedSession(" ", AuthSessionContext::new))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
    }
}
