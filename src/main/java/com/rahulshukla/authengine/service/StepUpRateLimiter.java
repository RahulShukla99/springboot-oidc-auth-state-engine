package com.rahulshukla.authengine.service;

public interface StepUpRateLimiter {
    void checkAllowed(String username);
}
