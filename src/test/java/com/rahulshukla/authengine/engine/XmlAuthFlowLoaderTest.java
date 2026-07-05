package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.exception.AuthFlowValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlAuthFlowLoaderTest {

    @Test
    void shouldLoadValidFlow() {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:valid-auth-flow.xml");

        var flow = loader.load();

        assertThat(flow.name()).isEqualTo("valid-test-flow");
        assertThat(flow.initialState().id()).isEqualTo("START");
        assertThat(flow.finalStates()).extracting("id").containsExactly("AUTH_SUCCESS");
    }

    @Test
    void shouldLoadStepUpMfaFlow() {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:step-up-auth-flow.xml");

        var flow = loader.load();

        assertThat(flow.name()).isEqualTo("step-up-mfa-flow");
        assertThat(flow.initialState().id()).isEqualTo("START");
        assertThat(flow.finalStates()).extracting("id").containsExactly("STEP_UP_SUCCESS", "AUTH_FAILED");
        assertThat(flow.states()).extracting("id").containsExactly(
                "START",
                "REDIRECT_TO_IDP",
                "VALIDATE_TOKEN",
                "LOAD_USER_PROFILE",
                "REQUIRE_MFA",
                "STEP_UP_SUCCESS",
                "AUTH_FAILED"
        );
    }

    @Test
    void shouldRejectDuplicateStates() {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:duplicate-state-auth-flow.xml");

        assertThatThrownBy(loader::load)
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Duplicate state id: START");
    }

    @Test
    void shouldRejectMissingTransitionTarget() {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:missing-target-auth-flow.xml");

        assertThatThrownBy(loader::load)
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Transition target MISSING does not exist");
    }

    @Test
    void shouldRejectBlankStateId() {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:blank-state-id-auth-flow.xml");

        assertThatThrownBy(loader::load)
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("State id must not be blank");
    }
}
