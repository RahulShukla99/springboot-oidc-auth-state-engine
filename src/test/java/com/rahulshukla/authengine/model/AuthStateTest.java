package com.rahulshukla.authengine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthStateTest {

    @Test
    void shouldCopyTransitionsAndDetectOutgoingTransitions() {
        AuthState state = new AuthState("START", true, false, List.of(new AuthTransition("GO", "END")));

        assertThat(state.transitions()).hasSize(1);
        assertThat(state.hasOutgoingTransitions()).isTrue();
    }

    @Test
    void shouldUseEmptyTransitionsWhenConfiguredWithNull() {
        AuthState state = new AuthState("END", false, true, null);

        assertThat(state.transitions()).isEmpty();
        assertThat(state.hasOutgoingTransitions()).isFalse();
    }
}
