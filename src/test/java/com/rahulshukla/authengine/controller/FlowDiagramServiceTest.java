package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.service.FlowDiagramService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowDiagramServiceTest {

    private final FlowDiagramService service = new FlowDiagramService();

    @Test
    void shouldRenderGraphvizBackedFlowAsInlineSvg() {
        AuthController.FlowResponse flow = new AuthController.FlowResponse(
                "oidc-post-login-auth-flow",
                "START",
                List.of("AUTH_SUCCESS", "AUTH_FAILED"),
                List.of(),
                List.of(new AuthController.TransitionView("START", "LOGIN_REQUESTED", "REDIRECT_TO_IDP"))
        );

        String html = service.renderFlowHtml(flow);

        assertThat(html).contains("data-renderer=\"graphviz\"")
                .contains("<svg")
                .contains("LOGIN_REQUESTED")
                .doesNotContain("stateDiagram-v2");
    }

    @Test
    void shouldRenderMermaidFallbackForUnsupportedFlow() {
        AuthController.FlowResponse flow = new AuthController.FlowResponse(
                "custom-flow",
                "START",
                List.of("END"),
                List.of(),
                List.of(new AuthController.TransitionView("START", "GO", "END"))
        );

        String html = service.renderFlowHtml(flow);

        assertThat(html).contains("data-renderer=\"text\"")
                .contains("stateDiagram-v2")
                .contains("START --&gt; END")
                .contains("GO");
    }

    @Test
    void shouldWrapMissingClasspathResourceAsIllegalStateException() {
        assertThatThrownBy(() -> {
            var method = FlowDiagramService.class.getDeclaredMethod("readClasspathResource", String.class);
            method.setAccessible(true);
            method.invoke(service, "diagrams/does-not-exist.svg");
        }).hasCauseInstanceOf(IllegalStateException.class);
    }
}
