package com.rahulshukla.authengine.config;

import com.rahulshukla.authengine.infrastructure.InMemoryOAuth2AuthorizedClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;

@Configuration
public class OAuth2ClientPersistenceConfig {

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    OAuth2AuthorizedClientManager oauth2AuthorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                               InMemoryOAuth2AuthorizedClientRepository authorizedClientRepository) {
        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return manager;
    }
}
