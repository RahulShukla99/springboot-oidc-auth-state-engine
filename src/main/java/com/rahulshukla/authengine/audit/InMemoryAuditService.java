package com.rahulshukla.authengine.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Service
@Slf4j
public class InMemoryAuditService {
    private final int maxRecords;
    private final Deque<AuthAuditRecord> records = new ArrayDeque<>();

    public InMemoryAuditService() {
        this(100);
    }

    public InMemoryAuditService(@Value("${auth.audit.max-records:100}") int maxRecords) {
        if (maxRecords < 1) {
            throw new IllegalArgumentException("auth.audit.max-records must be greater than zero");
        }
        this.maxRecords = maxRecords;
    }

    public synchronized void record(AuthAuditRecord record) {
        records.addLast(record);
        while (records.size() > maxRecords) {
            records.removeFirst();
        }
        log.info("auth transition correlationId={} username={} fromState={} event={} toState={} outcome={}",
                record.correlationId(), record.username(), record.fromState(), record.event(), record.toState(), record.outcome());
    }

    public synchronized List<AuthAuditRecord> recentRecords() {
        return List.copyOf(records);
    }
}
