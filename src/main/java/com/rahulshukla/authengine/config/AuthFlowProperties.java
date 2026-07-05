package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "auth")
public record AuthFlowProperties(Map<String, String> flows) {
    public AuthFlowProperties {
        flows = flows == null || flows.isEmpty()
                ? Map.of("login", "classpath:auth-flow.xml")
                : Map.copyOf(flows);
    }
}
