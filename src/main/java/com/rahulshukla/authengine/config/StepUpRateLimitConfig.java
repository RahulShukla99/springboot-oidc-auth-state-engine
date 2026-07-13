package com.rahulshukla.authengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class StepUpRateLimitConfig {
    @Bean
    Clock stepUpRateLimitClock() {
        return Clock.systemUTC();
    }
}
