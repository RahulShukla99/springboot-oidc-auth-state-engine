package com.rahulshukla.authengine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionContextTest {

    @Test
    void shouldCopyCollectionsAndExposeImmutableViews() {
        AuthSessionContext context = new AuthSessionContext("corr-1");
        context.setGroupsOrRoles(List.of("APP_USER"));
        context.addTransition(new TransitionHistoryEntry("START", "LOGIN_REQUESTED", "END", java.time.Instant.now()));

        assertThat(context.getGroupsOrRoles()).containsExactly("APP_USER");
        assertThat(context.getTransitionHistory()).hasSize(1);
    }

    @Test
    void shouldCreateRandomCorrelationIdWhenUsingDefaultConstructor() {
        AuthSessionContext context = new AuthSessionContext();

        assertThat(context.getCorrelationId()).isNotBlank();
        context.setGroupsOrRoles(null);
        assertThat(context.getGroupsOrRoles()).isEmpty();
    }
}
