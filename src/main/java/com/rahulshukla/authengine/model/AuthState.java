package com.rahulshukla.authengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record AuthState(String id, boolean initial, boolean finalState, List<AuthTransition> transitions) {
    public AuthState {
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    @JsonIgnore
    public boolean hasOutgoingTransitions() {
        return !transitions.isEmpty();
    }
}
