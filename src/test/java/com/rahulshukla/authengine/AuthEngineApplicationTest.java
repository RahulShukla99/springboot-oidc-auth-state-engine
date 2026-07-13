package com.rahulshukla.authengine;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AuthEngineApplicationTest {

    @Test
    void shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            AuthEngineApplication.main(new String[0]);
            springApplication.verify(() -> SpringApplication.run(AuthEngineApplication.class, new String[0]));
        }
    }
}
