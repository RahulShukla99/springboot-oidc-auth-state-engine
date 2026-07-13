package com.rahulshukla.authengine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowTest {

    @Test
    void shouldCopyStatesAndExposeInitialAndFinalStates() {
        AuthFlow flow = new AuthFlow("demo", List.of(
                new AuthState("START", true, false, List.of()),
                new AuthState("END", false, true, List.of())
        ));

        assertThat(flow.states()).hasSize(2);
        assertThat(flow.initialState().id()).isEqualTo("START");
        assertThat(flow.finalStates()).extracting(AuthState::id).containsExactly("END");
    }

    @Test
    void shouldUseEmptyStatesWhenConfiguredWithNull() {
        AuthFlow flow = new AuthFlow("demo", null);

        assertThat(flow.states()).isEmpty();
        assertThatThrownBy(flow::initialState)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Auth flow has no initial state");
    }
}
