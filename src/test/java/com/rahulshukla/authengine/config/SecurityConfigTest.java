package com.rahulshukla.authengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowDefaultBrowserFlowViewWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/flow/view"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowStepUpBrowserFlowViewWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/flow/step-up/view"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeMetricsWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/actuator/metrics").with(user("metrics-user")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeSwaggerUiWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/swagger-ui.html").with(user("swagger-user")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/swagger-ui/index.html"));
    }
}
