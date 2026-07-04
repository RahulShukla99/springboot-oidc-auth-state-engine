package com.rahulshukla.authengine.model;

import java.time.Instant;

public record TransitionHistoryEntry(String fromState, String event, String toState, Instant timestamp) {
}
