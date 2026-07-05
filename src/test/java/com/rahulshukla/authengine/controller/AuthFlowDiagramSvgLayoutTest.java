package com.rahulshukla.authengine.controller;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowDiagramSvgLayoutTest {

    private static final Path LOGIN_SVG_PATH = Path.of("src", "main", "resources", "diagrams", "oidc-post-login-auth-flow.svg");
    private static final Path STEP_UP_SVG_PATH = Path.of("src", "main", "resources", "diagrams", "step-up-mfa-flow.svg");
    private static final double MIN_SOURCE_GAP = 10.0;
    private static final double MIN_FAILURE_GAP = 20.0;
    private static final double MIN_RIGHT_LANE_Y_GAP = 8.0;
    private static final double MAX_RIGHT_LANE_Y_GAP = 40.0;
    private static final double MAX_ARROW_TO_FAILURE_GAP = 18.0;
    private static final Pattern HORIZONTAL_PATH = Pattern.compile("M\\s+([0-9.]+)\\s+([0-9.]+)\\s+L\\s+([0-9.]+)\\s+([0-9.]+)");

    @Test
    void shouldKeepTokenInvalidLabelInRightLaneForLoginFlow() throws Exception {
        Document document = parseSvg(LOGIN_SVG_PATH);

        assertTransitionLabelFitsRightLane(document, "TOKEN_INVALID", 600.0, 260.0, 850.0, 70.0);
    }

    @Test
    void shouldKeepUnauthorizedLabelInRightLaneForLoginFlow() throws Exception {
        Document document = parseSvg(LOGIN_SVG_PATH);

        assertTransitionLabelFitsRightLane(document, "USER_NOT_AUTHORIZED", 595.0, 460.0, 860.0, 100.0);
    }

    @Test
    void shouldDefineExplicitStateNodeColorsWithoutCssDependency() throws Exception {
        Document document = parseSvg(LOGIN_SVG_PATH);

        Element startNode = ellipseAt(document, "450", "90");
        Element successNode = ellipseAt(document, "450", "620");
        Element failureNode = ellipseAt(document, "850", "290");

        assertThat(startNode.getAttribute("fill")).isEqualTo("#dbeafe");
        assertThat(startNode.getAttribute("stroke")).isEqualTo("#2563eb");
        assertThat(successNode.getAttribute("fill")).isEqualTo("#dcfce7");
        assertThat(successNode.getAttribute("stroke")).isEqualTo("#16a34a");
        assertThat(failureNode.getAttribute("fill")).isEqualTo("#fee2e2");
        assertThat(failureNode.getAttribute("stroke")).isEqualTo("#dc2626");
    }

    @Test
    void shouldKeepStepUpFailureLabelsInRightLane() throws Exception {
        Document document = parseSvg(STEP_UP_SVG_PATH);

        assertTransitionLabelFitsRightLane(document, "TOKEN_INVALID", 600.0, 260.0, 850.0, 70.0);
        assertTransitionLabelFitsRightLane(document, "MFA_FAILED", 585.0, 460.0, 860.0, 100.0);
    }

    @Test
    void shouldExtendLoginFailureArrowsCloseToFailureNodes() throws Exception {
        Document document = parseSvg(LOGIN_SVG_PATH);

        assertArrowEndsNearFailureNode(document, 290.0, 850.0, 70.0);
        assertArrowEndsNearFailureNode(document, 490.0, 860.0, 100.0);
    }

    @Test
    void shouldExtendStepUpFailureArrowsCloseToFailureNodes() throws Exception {
        Document document = parseSvg(STEP_UP_SVG_PATH);

        assertArrowEndsNearFailureNode(document, 290.0, 850.0, 70.0);
        assertArrowEndsNearFailureNode(document, 490.0, 860.0, 100.0);
    }

    private void assertTransitionLabelFitsRightLane(
            Document document,
            String label,
            double sourceRightEdge,
            double sourceTop,
            double failureCenterX,
            double failureRadiusX
    ) {
        Element labelElement = textElement(document, label);
        double labelX = Double.parseDouble(labelElement.getAttribute("x"));
        double labelY = Double.parseDouble(labelElement.getAttribute("y"));
        double failureLeftEdge = failureCenterX - failureRadiusX;

        assertThat(labelElement.getAttribute("text-anchor"))
                .as("%s should grow to the right from the branch lane", label)
                .isEqualTo("start");
        assertThat(labelX)
                .as("%s should sit to the right of its source node", label)
                .isGreaterThanOrEqualTo(sourceRightEdge + MIN_SOURCE_GAP);
        assertThat(labelX)
                .as("%s should still start before AUTH_FAILED begins", label)
                .isLessThanOrEqualTo(failureLeftEdge - MIN_FAILURE_GAP);
        assertThat(labelY)
                .as("%s should stay within the branch lane instead of floating into the box above", label)
                .isGreaterThanOrEqualTo(sourceTop + MIN_RIGHT_LANE_Y_GAP);
        assertThat(labelY)
                .as("%s should remain near the source lane, not drop into the next state", label)
                .isLessThanOrEqualTo(sourceTop + MAX_RIGHT_LANE_Y_GAP);
    }

    private void assertArrowEndsNearFailureNode(Document document, double y, double failureCenterX, double failureRadiusX) {
        double arrowEndX = horizontalArrowEndX(document, y);
        double failureLeftEdge = failureCenterX - failureRadiusX;

        assertThat(failureLeftEdge - arrowEndX)
                .as("arrow at y=%s should end close to the failure node", y)
                .isLessThanOrEqualTo(MAX_ARROW_TO_FAILURE_GAP);
    }

    private double horizontalArrowEndX(Document document, double y) {
        NodeList paths = document.getElementsByTagName("path");
        for (int index = 0; index < paths.getLength(); index++) {
            Element path = (Element) paths.item(index);
            Matcher matcher = HORIZONTAL_PATH.matcher(path.getAttribute("d"));
            if (matcher.matches()) {
                double startY = Double.parseDouble(matcher.group(2));
                double endX = Double.parseDouble(matcher.group(3));
                double endY = Double.parseDouble(matcher.group(4));
                if (Double.compare(startY, y) == 0 && Double.compare(endY, y) == 0) {
                    return endX;
                }
            }
        }
        throw new IllegalArgumentException("Unable to find horizontal arrow at y=" + y);
    }

    private Document parseSvg(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private Element textElement(Document document, String value) {
        NodeList textNodes = document.getElementsByTagName("text");
        for (int index = 0; index < textNodes.getLength(); index++) {
            Element element = (Element) textNodes.item(index);
            if (value.equals(element.getTextContent().trim())) {
                return element;
            }
        }
        throw new IllegalArgumentException("Unable to find text node for " + value);
    }

    private Element ellipseAt(Document document, String cx, String cy) {
        NodeList ellipses = document.getElementsByTagName("ellipse");
        for (int index = 0; index < ellipses.getLength(); index++) {
            Element element = (Element) ellipses.item(index);
            if (cx.equals(element.getAttribute("cx")) && cy.equals(element.getAttribute("cy"))) {
                return element;
            }
        }
        throw new IllegalArgumentException("Unable to find ellipse at " + cx + "," + cy);
    }
}
