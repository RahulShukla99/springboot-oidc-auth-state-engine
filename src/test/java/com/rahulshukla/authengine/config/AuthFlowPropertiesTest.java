package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFlowPropertiesTest {

    @Test
    void shouldDefaultToLoginFlowWhenConfiguredWithNullOrEmptyMap() {
        assertThat(new AuthFlowProperties(null).flows()).containsEntry("login", "classpath:auth-flow.xml");
        assertThat(new AuthFlowProperties(Map.of()).flows()).containsEntry("login", "classpath:auth-flow.xml");
    }

    @Test
    void shouldCopyConfiguredFlowMap() {
        AuthFlowProperties properties = new AuthFlowProperties(Map.of("login", "classpath:custom.xml"));

        assertThat(properties.flows()).containsEntry("login", "classpath:custom.xml");
        assertThatThrownBy(() -> properties.flows().put("other", "classpath:other.xml"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
