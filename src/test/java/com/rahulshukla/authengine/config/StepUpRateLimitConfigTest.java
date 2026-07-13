package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StepUpRateLimitConfigTest {

    @Test
    void shouldCreateUtcClock() {
        StepUpRateLimitConfig config = new StepUpRateLimitConfig();

        assertThat(config.stepUpRateLimitClock().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
