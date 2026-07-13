package com.rahulshukla.authengine.engine;

public interface TransitionRuleResolver {
    TransitionRule resolve(String flowName, String event);
}
