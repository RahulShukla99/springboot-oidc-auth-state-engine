package com.rahulshukla.authengine.service;

import com.rahulshukla.authengine.controller.AuthController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FlowDiagramService {
    private static final String OIDC_POST_LOGIN_FLOW = "oidc-post-login-auth-flow";
    private static final String STEP_UP_MFA_FLOW = "step-up-mfa-flow";

    public String renderFlowHtml(AuthController.FlowResponse flow) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    :root { color: #101828; font-family: Arial, sans-serif; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      background: #ffffff;
                      color: #101828;
                    }
                    .diagram-shell {
                      min-height: 100vh;
                      display: flex;
                      align-items: flex-start;
                      justify-content: center;
                      padding: 12px;
                    }
                    .diagram-frame {
                      margin: 0;
                      width: min(100%%, 920px);
                    }
                    .diagram-frame svg {
                      display: block;
                      width: 100%%;
                      height: auto;
                    }
                    .sr-only {
                      position: absolute;
                      width: 1px;
                      height: 1px;
                      padding: 0;
                      margin: -1px;
                      overflow: hidden;
                      clip: rect(0, 0, 0, 0);
                      white-space: nowrap;
                      border: 0;
                    }
                  </style>
                </head>
                <body>
                  <main class="diagram-shell">
                    <figure class="diagram-frame" data-renderer="%s">
                      <figcaption class="sr-only">%s</figcaption>
                      %s
                    </figure>
                  </main>
                </body>
                </html>
                """.formatted(
                flow.flowName(),
                rendererName(flow),
                accessibleSummary(flow),
                renderDiagramMarkup(flow)
        );
    }

    private String renderDiagramMarkup(AuthController.FlowResponse flow) {
        String graphvizSvgResource = graphvizSvgResource(flow.flowName());
        if (graphvizSvgResource != null) {
            return inlineSvgMarkup(readClasspathResource(graphvizSvgResource));
        }
        return "<pre>" + escapeHtml(toMermaid(flow)) + "</pre>";
    }

    private String rendererName(AuthController.FlowResponse flow) {
        return graphvizSvgResource(flow.flowName()) != null ? "graphviz" : "text";
    }

    private String accessibleSummary(AuthController.FlowResponse flow) {
        return flow.flowName()
                + " diagram. Initial state " + flow.initialState()
                + ". Final states " + String.join(", ", flow.finalStates()) + ".";
    }

    private String graphvizSvgResource(String flowName) {
        return switch (flowName) {
            case OIDC_POST_LOGIN_FLOW -> "diagrams/oidc-post-login-auth-flow.svg";
            case STEP_UP_MFA_FLOW -> "diagrams/step-up-mfa-flow.svg";
            default -> null;
        };
    }

    private String toMermaid(AuthController.FlowResponse flow) {
        StringBuilder diagram = new StringBuilder("stateDiagram-v2\n");
        diagram.append("    [*] --> ").append(flow.initialState()).append('\n');
        flow.transitions().forEach(transition -> diagram.append("    ")
                .append(transition.fromState())
                .append(" --> ")
                .append(transition.toState())
                .append(": ")
                .append(transition.event())
                .append('\n'));
        flow.finalStates().forEach(state -> diagram.append("    ").append(state).append(" --> [*]\n"));
        return diagram.toString();
    }

    private String inlineSvgMarkup(String svg) {
        String normalizedSvg = svg.replaceFirst("^\\s*<\\?xml[^>]*>\\s*", "");
        return normalizedSvg.replaceFirst("<svg\\b", "<svg class=\"graphviz-diagram\"");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String readClasspathResource(String resourcePath) {
        try (var inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render flow diagram from resource " + resourcePath, exception);
        }
    }
}
