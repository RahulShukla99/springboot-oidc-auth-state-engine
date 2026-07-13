package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.exception.AuthFlowValidationException;
import com.rahulshukla.authengine.model.AuthFlow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void shouldRejectBlankFlowName() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"\">
                  <states>
                    <state id=\"START\" initial=\"true\" final=\"false\"/>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Auth flow name must not be blank");
    }

    @Test
    void shouldRejectMissingInitialState() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"missing-initial\">
                  <states>
                    <state id=\"START\" initial=\"false\" final=\"false\"/>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Auth flow must contain exactly one initial state");
    }

    @Test
    void shouldRejectEmptyStates() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"empty-states\">
                  <states/>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Auth flow must contain at least one state");
    }

    @Test
    void shouldRejectFinalStateWithOutgoingTransition() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"final-with-transition\">
                  <states>
                    <state id=\"START\" initial=\"true\" final=\"false\">
                      <transitions>
                        <transition event=\"GO\" target=\"END\"/>
                      </transitions>
                    </state>
                    <state id=\"END\" initial=\"false\" final=\"true\">
                      <transitions>
                        <transition event=\"SHOULD_NOT_EXIST\" target=\"START\"/>
                      </transitions>
                    </state>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Final state END must not have outgoing transitions");
    }

    @Test
    void shouldRejectBlankTransitionEventAndTarget() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"blank-transition\">
                  <states>
                    <state id=\"START\" initial=\"true\" final=\"false\">
                      <transitions>
                        <transition event=\" \" target=\"END\"/>
                      </transitions>
                    </state>
                    <state id=\"END\" initial=\"false\" final=\"true\"/>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Transition event must not be blank for state START");

        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"blank-transition-target\">
                  <states>
                    <state id=\"START\" initial=\"true\" final=\"false\">
                      <transitions>
                        <transition event=\"GO\" target=\" \"/>
                      </transitions>
                    </state>
                    <state id=\"END\" initial=\"false\" final=\"true\"/>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Transition target must not be blank for state START");
    }

    @Test
    void shouldWrapUnreadableXmlAsValidationException() {
        assertThatThrownBy(() -> new XmlAuthFlowLoader("file:///does/not/exist.xml").load())
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Invalid auth flow XML");
    }

    @Test
    void shouldRejectNullFlowDuringValidation() throws Exception {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:valid-auth-flow.xml");
        Method validate = XmlAuthFlowLoader.class.getDeclaredMethod("validate", AuthFlow.class);
        validate.setAccessible(true);

        assertThatThrownBy(() -> validate.invoke(loader, new Object[]{null}))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(AuthFlowValidationException.class)
                .hasRootCauseMessage("Auth flow must contain at least one state");
    }

    @Test
    void shouldTreatNullTextAsBlankWhenValidatingPrivateHelper() throws Exception {
        XmlAuthFlowLoader loader = new XmlAuthFlowLoader("classpath:valid-auth-flow.xml");
        Method isBlank = XmlAuthFlowLoader.class.getDeclaredMethod("isBlank", String.class);
        isBlank.setAccessible(true);

        assertThat(isBlank.invoke(loader, new Object[]{null})).isEqualTo(true);
    }

    @Test
    void shouldRejectDuplicateTransitionEvent() throws IOException {
        assertThatThrownBy(() -> loadXml("""
                <authFlow name=\"duplicate-transition\">
                  <states>
                    <state id=\"START\" initial=\"true\" final=\"false\">
                      <transitions>
                        <transition event=\"GO\" target=\"END\"/>
                        <transition event=\"GO\" target=\"END\"/>
                      </transitions>
                    </state>
                    <state id=\"END\" initial=\"false\" final=\"true\"/>
                  </states>
                </authFlow>
                """))
                .isInstanceOf(AuthFlowValidationException.class)
                .hasMessageContaining("Duplicate transition event GO for state START");
    }

    private AuthFlow loadXml(String xml) throws IOException {
        Path file = Files.createTempFile("flow-", ".xml");
        Files.writeString(file, xml);
        return new XmlAuthFlowLoader(file.toUri().toString()).load();
    }
}
