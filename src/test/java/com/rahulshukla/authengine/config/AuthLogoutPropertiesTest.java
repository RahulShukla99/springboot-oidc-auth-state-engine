package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthLogoutPropertiesTest {

    @Test
    void shouldPreserveConfiguredRedirectUri() {
        AuthLogoutProperties properties = new AuthLogoutProperties("http://example.test/");

        assertThat(properties.postLogoutRedirectUri()).isEqualTo("http://example.test/");
    }
}
