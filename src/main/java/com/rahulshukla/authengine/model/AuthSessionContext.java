package com.rahulshukla.authengine.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AuthSessionContext {
    private final String correlationId;
    private String username;
    private String fullName;
    private boolean emailVerified;
    @Setter(AccessLevel.NONE)
    private List<String> groupsOrRoles = new ArrayList<>();
    private String currentState;
    private String finalState;
    private String failureReason;
    @Setter(AccessLevel.NONE)
    private final List<TransitionHistoryEntry> transitionHistory = new ArrayList<>();

    public AuthSessionContext() {
        this(UUID.randomUUID().toString());
    }

    public AuthSessionContext(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<String> getGroupsOrRoles() {
        return List.copyOf(groupsOrRoles);
    }

    public void setGroupsOrRoles(List<String> groupsOrRoles) {
        this.groupsOrRoles = groupsOrRoles == null ? new ArrayList<>() : new ArrayList<>(groupsOrRoles);
    }

    public List<TransitionHistoryEntry> getTransitionHistory() {
        return List.copyOf(transitionHistory);
    }

    public void addTransition(TransitionHistoryEntry entry) {
        transitionHistory.add(entry);
    }
}
