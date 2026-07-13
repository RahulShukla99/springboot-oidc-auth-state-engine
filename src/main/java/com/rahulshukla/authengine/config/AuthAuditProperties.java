package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.audit")
public record AuthAuditProperties(int maxRecords) {
}
