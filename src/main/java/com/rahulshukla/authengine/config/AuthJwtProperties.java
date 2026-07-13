package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record AuthJwtProperties(long allowedClockSkewSeconds) {
}
