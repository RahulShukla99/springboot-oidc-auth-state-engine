package com.rahulshukla.authengine.model;

import java.util.List;

public record AuthFlow(String name, List<AuthState> states) {
    public AuthFlow {
        states = states == null ? List.of() : List.copyOf(states);
    }

    public AuthState initialState() {
        return states.stream().filter(AuthState::initial).findFirst()
                .orElseThrow(() -> new IllegalStateException("Auth flow has no initial state"));
    }

    public List<AuthState> finalStates() {
        return states.stream().filter(AuthState::finalState).toList();
    }
}
