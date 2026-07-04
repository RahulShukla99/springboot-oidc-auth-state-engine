package com.rahulshukla.authengine.audit;

import java.time.Instant;

public record AuthAuditRecord(
        String correlationId,
        String username,
        String fromState,
        String event,
        String toState,
        Instant timestamp,
        String outcome
) {
}
