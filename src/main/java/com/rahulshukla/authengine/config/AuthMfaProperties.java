package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.mfa")
public record AuthMfaProperties(String challengeCode) {
}
