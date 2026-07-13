package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;
import com.rahulshukla.authengine.infrastructure.InMemoryOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ClientPersistenceConfigTest {

    @Test
    void shouldCreateInMemoryRepositoryAndManagerWhenClientRegistrationExists() {
        OAuth2ClientPersistenceConfig config = new OAuth2ClientPersistenceConfig();
        ClientRegistrationRepository registrations = new InMemoryClientRegistrationRepository(clientRegistration());

        OAuth2AuthorizedClientManager manager = config.oauth2AuthorizedClientManager(registrations, new InMemoryOAuth2AuthorizedClientRepository());
        assertThat(manager).isNotNull();
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("auth0")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/auth0")
                .scope("openid")
                .authorizationUri("https://example.us.auth0.com/authorize")
                .tokenUri("https://example.us.auth0.com/oauth/token")
                .issuerUri("https://example.us.auth0.com/")
                .userInfoUri("https://example.us.auth0.com/userinfo")
                .userNameAttributeName("sub")
                .clientName("Auth0")
                .build();
    }
}
