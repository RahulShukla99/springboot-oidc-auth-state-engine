package com.rahulshukla.authengine.engine;

import com.rahulshukla.authengine.exception.AuthFlowValidationException;

import java.util.Map;

public class AuthFlowRegistry {
    private final Map<String, AuthStateEngine> enginesByName;

    public AuthFlowRegistry(Map<String, AuthStateEngine> enginesByName) {
        if (enginesByName == null || enginesByName.isEmpty()) {
            throw new AuthFlowValidationException("At least one authentication flow must be configured");
        }
        this.enginesByName = Map.copyOf(enginesByName);
    }

    public AuthStateEngine getRequiredEngine(String flowName) {
        AuthStateEngine engine = enginesByName.get(flowName);
        if (engine == null) {
            throw new AuthFlowValidationException("Authentication flow is not configured: " + flowName);
        }
        return engine;
    }

    public Map<String, AuthStateEngine> enginesByName() {
        return enginesByName;
    }
}
