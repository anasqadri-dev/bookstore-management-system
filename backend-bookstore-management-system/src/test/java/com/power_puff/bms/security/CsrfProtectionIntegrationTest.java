package com.power_puff.bms.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesSessionBoundTokenWithoutExposingItInACookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertFalse(setCookie != null && setCookie.contains("XSRF-TOKEN"));
    }

    @Test
    void rejectsUnsafeRequestWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnsafeRequestWhenTokenDoesNotMatchSession() throws Exception {
        MvcResult tokenResult = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", "invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void validSessionTokenPassesCsrfValidationBeforeAuthenticationIsChecked()
            throws Exception {
        MvcResult tokenResult = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        assertNotNull(session);
        String token = com.jayway.jsonpath.JsonPath.read(
                tokenResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
