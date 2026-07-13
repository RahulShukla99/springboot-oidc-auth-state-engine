package com.rahulshukla.authengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.time.Duration;

@Configuration
public class AuthJwtDecoderConfig {
    @Bean
    OidcIdTokenDecoderFactory oidcIdTokenDecoderFactory(AuthJwtProperties authJwtProperties) {
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(clientRegistration -> tokenValidator(clientRegistration, authJwtProperties));
        return factory;
    }

    static JwtTimestampValidator clockSkewValidator(Duration clockSkew) {
        return new JwtTimestampValidator(clockSkew);
    }

    private static OAuth2TokenValidator<Jwt> tokenValidator(ClientRegistration clientRegistration, AuthJwtProperties authJwtProperties) {
        JwtTimestampValidator timestampValidator = clockSkewValidator(Duration.ofSeconds(authJwtProperties.allowedClockSkewSeconds()));
        return new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(clientRegistration.getProviderDetails().getIssuerUri()),
                timestampValidator
        );
    }
}
