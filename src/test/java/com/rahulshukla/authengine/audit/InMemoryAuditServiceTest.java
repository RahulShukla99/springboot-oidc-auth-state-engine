package com.rahulshukla.authengine.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuditServiceTest {

    @Test
    void shouldKeepOnlyConfiguredNumberOfRecentRecords() {
        InMemoryAuditService service = new InMemoryAuditService(new com.rahulshukla.authengine.config.AuthAuditProperties(2));

        service.record(record("corr-1"));
        service.record(record("corr-2"));
        service.record(record("corr-3"));

        assertThat(service.recentRecords()).extracting(AuthAuditRecord::correlationId)
                .containsExactly("corr-2", "corr-3");
    }

    @Test
    void shouldRejectNonPositiveConfiguredMaxRecords() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new InMemoryAuditService(new com.rahulshukla.authengine.config.AuthAuditProperties(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.audit.max-records must be greater than zero");
    }

    private AuthAuditRecord record(String correlationId) {
        return new AuthAuditRecord(correlationId, "user@example.com", "START", "LOGIN_REQUESTED", "REDIRECT_TO_IDP", Instant.now(), "SUCCESS");
    }
}
