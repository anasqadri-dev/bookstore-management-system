package com.power_puff.bms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvalidRequestPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "validation-admin", roles = "ADMIN")
    void invalidCreateRequestsAcrossCoreModulesReturnBadRequestWithoutSavingRows()
            throws Exception {
        Map<String, Long> before = tableCounts();

        assertBadRequest("/api/products", """
                {
                  "sku":"",
                  "title":"Invalid Product",
                  "author":null,
                  "price":0,
                  "quantity":-1,
                  "minStock":-1
                }
                """);

        assertBadRequest("/api/sales", """
                {"customerName":"Invalid Sale","items":[]}
                """);

        assertBadRequest("/api/school-orders", """
                {"schoolId":1,"items":[]}
                """);

        assertBadRequest("/api/purchase-orders", """
                {"supplierId":1,"items":[]}
                """);

        assertBadRequest("/api/suppliers", """
                {"name":" ","phone":null,"address":null}
                """);

        assertBadRequest("/api/users", """
                {
                  "username":"invalid-validation-user",
                  "password":"tiny",
                  "fullName":"Invalid User"
                }
                """);

        assertEquals(before, tableCounts(),
                "Rejected create requests must not leave partial rows in the database.");
    }

    private void assertBadRequest(String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Map<String, Long> tableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : new String[]{
                "products", "sales", "school_orders",
                "purchase_orders", "suppliers", "users"
        }) {
            counts.put(table, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Long.class));
        }
        return counts;
    }
}
