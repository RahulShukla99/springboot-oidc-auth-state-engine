package com.rahulshukla.authengine.controller;

import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.service.FlowDiagramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for browser flow diagram views.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthFlowViewController {
    private static final String DEFAULT_FLOW = "login";

    private final AuthFlowRegistry flowRegistry;
    private final AuthViewMapper viewMapper;
    private final FlowDiagramService diagramService;

    @GetMapping(value = "/auth/flow/view", produces = MediaType.TEXT_HTML_VALUE)
    public String flowView() {
        return flowView(DEFAULT_FLOW);
    }

    @GetMapping(value = "/auth/flow/{flowName}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String flowView(@PathVariable String flowName) {
        AuthFlow authFlow = flowRegistry.getRequiredEngine(flowName).flow();
        log.debug("flow html view requested flow={} stateCount={}", flowName, authFlow.states().size());
        return diagramService.renderFlowHtml(viewMapper.toFlowResponse(authFlow));
    }
}
