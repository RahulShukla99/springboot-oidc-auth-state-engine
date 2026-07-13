package com.rahulshukla.authengine.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                            ObjectProvider<OAuth2ClientProperties> oauth2ClientProperties,
                                            ObjectProvider<OAuth2AuthorizedClientRepository> authorizedClientRepository,
                                            AuthLogoutProperties authLogoutProperties) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/flow", "/auth/flow/view", "/auth/flow/*/view", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").authenticated()
                        .requestMatchers("/auth/success", "/auth/success/*", "/auth/session", "/auth/session/*", "/auth/audit", "/auth/step-up/verify").authenticated()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session.sessionFixation().migrateSession())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny()))
                .logout(logout -> logout.logoutSuccessHandler(auth0LogoutSuccessHandler(oauth2ClientProperties.getIfAvailable(OAuth2ClientProperties::new), authLogoutProperties)));

        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> {
                oauth.defaultSuccessUrl("/auth/success", true);
                authorizedClientRepository.ifAvailable(oauth::authorizedClientRepository);
            });
        }

        return http.build();
    }

    @Bean
    Auth0LogoutSuccessHandler auth0LogoutSuccessHandler(OAuth2ClientProperties oauth2ClientProperties,
                                                        AuthLogoutProperties authLogoutProperties) {
        var registration = oauth2ClientProperties.getRegistration().get("auth0");
        var provider = oauth2ClientProperties.getProvider().get("auth0");
        String clientId = registration == null ? null : registration.getClientId();
        String issuerUri = provider == null ? null : provider.getIssuerUri();
        return new Auth0LogoutSuccessHandler(issuerUri, clientId, authLogoutProperties.postLogoutRedirectUri());
    }
}
