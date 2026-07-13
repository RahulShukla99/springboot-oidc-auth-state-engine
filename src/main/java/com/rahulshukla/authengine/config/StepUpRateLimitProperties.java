package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth.step-up-rate-limit")
public record StepUpRateLimitProperties(Integer maxAttempts, Duration window) {
    public StepUpRateLimitProperties {
        maxAttempts = maxAttempts == null || maxAttempts < 1 ? 5 : maxAttempts;
        window = window == null ? Duration.ofSeconds(60) : window;
    }
}
