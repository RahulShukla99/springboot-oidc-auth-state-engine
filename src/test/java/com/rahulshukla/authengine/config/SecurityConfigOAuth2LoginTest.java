package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.auth0.client-id=test-client",
        "spring.security.oauth2.client.registration.auth0.client-secret=test-secret",
        "spring.security.oauth2.client.registration.auth0.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.auth0.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.auth0.redirect-uri=http://localhost/login/oauth2/code/auth0",
        "spring.security.oauth2.client.provider.auth0.authorization-uri=https://example.us.auth0.com/authorize",
        "spring.security.oauth2.client.provider.auth0.token-uri=https://example.us.auth0.com/oauth/token",
        "spring.security.oauth2.client.provider.auth0.user-info-uri=https://example.us.auth0.com/userinfo",
        "spring.security.oauth2.client.provider.auth0.user-name-attribute=sub",
        "spring.security.oauth2.client.provider.auth0.jwk-set-uri=https://example.us.auth0.com/.well-known/jwks.json"
})
@AutoConfigureMockMvc
class SecurityConfigOAuth2LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOauth2AuthorizationEndpointWhenClientRegistrationExists() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/auth0"))
                .andExpect(status().is3xxRedirection());
    }
}
