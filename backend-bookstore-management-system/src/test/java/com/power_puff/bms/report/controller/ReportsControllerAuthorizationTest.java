package com.power_puff.bms.report.controller;

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
class ReportsControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "reports-admin", roles = "ADMIN")
    void adminCanReadSalesAndInventoryReports() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-11")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-11-11"))
                .andExpect(jsonPath("$.data.totalSales").exists())
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.sales").isArray());

        mockMvc.perform(get("/api/reports/inventory")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProducts").exists())
                .andExpect(jsonPath("$.data.totalUnits").exists())
                .andExpect(jsonPath("$.data.lowStockProducts").exists())
                .andExpect(jsonPath("$.data.totalStockValue").exists())
                .andExpect(jsonPath("$.data.products").isArray());
    }

    @Test
    @WithMockUser(username = "reports-staff", roles = "STAFF")
    void staffCannotReadReports() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-11"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUsersCannotReadReports() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-11"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "reports-admin", roles = "ADMIN")
    void missingOrMalformedSalesDateParametersReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2026-11-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "reports-admin", roles = "ADMIN")
    void rejectsSalesReportWhenStartDateIsAfterEndDate() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2026-11-12")
                        .param("endDate", "2026-11-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
