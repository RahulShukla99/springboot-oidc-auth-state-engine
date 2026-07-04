package com.rahulshukla.authengine.config;

import com.rahulshukla.authengine.audit.InMemoryAuditService;
import com.rahulshukla.authengine.engine.AuthStateEngine;
import com.rahulshukla.authengine.engine.XmlAuthFlowLoader;
import com.rahulshukla.authengine.model.AuthFlow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthEngineConfig {
    @Bean
    AuthFlow authFlow(@Value("${auth.flow.path:classpath:auth-flow.xml}") String flowPath) {
        return new XmlAuthFlowLoader(flowPath).load();
    }

    @Bean
    AuthStateEngine authStateEngine(AuthFlow authFlow, InMemoryAuditService auditService) {
        return new AuthStateEngine(authFlow, auditService);
    }
}
