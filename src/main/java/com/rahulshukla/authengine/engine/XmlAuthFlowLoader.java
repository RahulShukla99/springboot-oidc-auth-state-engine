package com.rahulshukla.authengine.engine;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.rahulshukla.authengine.exception.AuthFlowValidationException;
import com.rahulshukla.authengine.model.AuthFlow;
import com.rahulshukla.authengine.model.AuthState;
import com.rahulshukla.authengine.model.AuthTransition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads and validates the XML-defined authentication flow.
 * <p>
 * This keeps the workflow configuration externalized while enforcing the core invariants:
 * one initial state, unique state IDs, valid transition targets, and no outgoing edges
 * from final states.
 */
public class XmlAuthFlowLoader {
    private final String flowPath;
    private final XmlMapper xmlMapper = new XmlMapper();

    public XmlAuthFlowLoader(String flowPath) {
        this.flowPath = flowPath;
    }

    public AuthFlow load() {
        try {
            Resource resource = new DefaultResourceLoader().getResource(flowPath);
            XmlAuthFlow xmlFlow = xmlMapper.readValue(resource.getInputStream(), XmlAuthFlow.class);
            AuthFlow flow = xmlFlow.toAuthFlow();
            validate(flow);
            return flow;
        } catch (AuthFlowValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AuthFlowValidationException("Invalid auth flow XML at " + flowPath + ": " + ex.getMessage(), ex);
        }
    }

    private void validate(AuthFlow flow) {
        if (flow == null || flow.states().isEmpty()) {
            throw new AuthFlowValidationException("Auth flow must contain at least one state");
        }
        if (isBlank(flow.name())) {
            throw new AuthFlowValidationException("Auth flow name must not be blank");
        }
        Set<String> ids = new HashSet<>();
        int initialCount = 0;
        for (AuthState state : flow.states()) {
            if (isBlank(state.id())) {
                throw new AuthFlowValidationException("State id must not be blank");
            }
            if (!ids.add(state.id())) {
                throw new AuthFlowValidationException("Duplicate state id: " + state.id());
            }
            if (state.initial()) {
                initialCount++;
            }
            if (state.finalState() && state.hasOutgoingTransitions()) {
                throw new AuthFlowValidationException("Final state " + state.id() + " must not have outgoing transitions");
            }
            Set<String> transitionEvents = new HashSet<>();
            state.transitions().forEach(transition -> {
                if (isBlank(transition.event())) {
                    throw new AuthFlowValidationException("Transition event must not be blank for state " + state.id());
                }
                if (isBlank(transition.target())) {
                    throw new AuthFlowValidationException("Transition target must not be blank for state " + state.id());
                }
                if (!transitionEvents.add(transition.event())) {
                    throw new AuthFlowValidationException("Duplicate transition event " + transition.event() + " for state " + state.id());
                }
            });
        }
        if (initialCount != 1) {
            throw new AuthFlowValidationException("Auth flow must contain exactly one initial state");
        }
        for (AuthState state : flow.states()) {
            state.transitions().forEach(transition -> {
                if (!ids.contains(transition.target())) {
                    throw new AuthFlowValidationException("Transition target " + transition.target() + " does not exist");
                }
            });
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JacksonXmlRootElement(localName = "authFlow")
    private static class XmlAuthFlow {
        @JacksonXmlProperty(isAttribute = true)
        private String name;
        @JacksonXmlElementWrapper(localName = "states")
        @JacksonXmlProperty(localName = "state")
        private List<XmlAuthState> states = List.of();

        AuthFlow toAuthFlow() {
            return new AuthFlow(name, states.stream().map(XmlAuthState::toAuthState).toList());
        }
    }

    private static class XmlAuthState {
        @JacksonXmlProperty(isAttribute = true)
        private String id;
        @JacksonXmlProperty(isAttribute = true)
        private boolean initial;
        @JacksonXmlProperty(isAttribute = true, localName = "final")
        private boolean finalState;
        @JacksonXmlElementWrapper(localName = "transitions")
        @JacksonXmlProperty(localName = "transition")
        private List<XmlAuthTransition> transitions = List.of();

        AuthState toAuthState() {
            return new AuthState(id, initial, finalState, transitions.stream().map(XmlAuthTransition::toAuthTransition).toList());
        }
    }

    private static class XmlAuthTransition {
        @JacksonXmlProperty(isAttribute = true)
        private String event;
        @JacksonXmlProperty(isAttribute = true)
        private String target;

        AuthTransition toAuthTransition() {
            return new AuthTransition(event, target);
        }
    }
}
