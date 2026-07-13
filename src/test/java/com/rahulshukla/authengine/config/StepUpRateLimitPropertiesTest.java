package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class StepUpRateLimitPropertiesTest {

    @Test
    void shouldApplyDefaultsWhenValuesAreMissing() {
        StepUpRateLimitProperties properties = new StepUpRateLimitProperties(null, null);

        assertThat(properties.maxAttempts()).isEqualTo(5);
        assertThat(properties.window()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void shouldKeepConfiguredValues() {
        StepUpRateLimitProperties properties = new StepUpRateLimitProperties(3, Duration.ofSeconds(15));

        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.window()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void shouldDefaultNonPositiveMaxAttemptsToFive() {
        StepUpRateLimitProperties properties = new StepUpRateLimitProperties(0, Duration.ofSeconds(15));

        assertThat(properties.maxAttempts()).isEqualTo(5);
        assertThat(properties.window()).isEqualTo(Duration.ofSeconds(15));
    }
}
