package com.rahulshukla.authengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.logout")
public record AuthLogoutProperties(String postLogoutRedirectUri) {
}
