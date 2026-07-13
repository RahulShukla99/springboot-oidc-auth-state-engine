package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthJwtPropertiesTest {

    @Test
    void shouldPreserveConfiguredClockSkewWhenBoundFromProperties() {
        AuthJwtProperties properties = new AuthJwtProperties(0);

        assertThat(properties.allowedClockSkewSeconds()).isEqualTo(0);
    }
}
