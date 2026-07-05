package com.rahulshukla.authengine.config;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthFlowRegistry;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.engine.XmlAuthFlowLoader;
import com.rahulshukla.authengine.model.AuthFlow;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(AuthFlowProperties.class)
public class AuthEngineConfig {
    @Bean
    AuthFlowRegistry authFlowRegistry(AuthFlowProperties properties, InMemoryAuditService auditService) {
        Map<String, AuthStateEngine> engines = properties.flows().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new AuthStateEngine(new XmlAuthFlowLoader(entry.getValue()).load(), auditService)
                ));
        return new AuthFlowRegistry(engines);
    }

    @Bean
    AuthStateEngine authStateEngine(AuthFlowRegistry registry) {
        return registry.getRequiredEngine("login");
    }

    @Bean
    AuthFlow authFlow(AuthStateEngine authStateEngine) {
        return authStateEngine.flow();
    }
}
