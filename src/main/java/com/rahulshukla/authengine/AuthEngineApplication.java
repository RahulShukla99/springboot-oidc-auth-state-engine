package com.rahulshukla.authengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthEngineApplication.class, args);
    }
}
