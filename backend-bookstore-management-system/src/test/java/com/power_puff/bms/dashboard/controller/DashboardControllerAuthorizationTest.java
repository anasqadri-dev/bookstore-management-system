package com.power_puff.bms.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "dashboard-admin", roles = "ADMIN")
    void adminCanRetrieveDashboardSummary() throws Exception {
        assertDashboardResponse();
    }

    @Test
    @WithMockUser(username = "dashboard-staff", roles = "STAFF")
    void staffCanRetrieveDashboardSummary() throws Exception {
        assertDashboardResponse();
    }

    @Test
    void unauthenticatedUsersCannotRetrieveDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private void assertDashboardResponse() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBooks").isNumber())
                .andExpect(jsonPath("$.data.todaySales").isNumber())
                .andExpect(jsonPath("$.data.lowStock").isNumber())
                .andExpect(jsonPath("$.data.suppliers").isNumber())
                .andExpect(jsonPath("$.data.recentSales").isArray())
                .andExpect(jsonPath("$.data.recentSales.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(5)));
    }
}
