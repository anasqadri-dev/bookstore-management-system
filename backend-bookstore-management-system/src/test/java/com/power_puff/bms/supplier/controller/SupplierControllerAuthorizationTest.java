package com.power_puff.bms.supplier.controller;

import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    private Integer createdSupplierId;

    @AfterEach
    void cleanUp() {
        if (createdSupplierId != null) {
            supplierRepository.deleteById(createdSupplierId);
        }
    }

    @Test
    @WithMockUser(username = "supplier-admin", roles = "ADMIN")
    void adminCanCreateAndListSuppliers() throws Exception {
        String supplierName = "Admin Test Supplier";

        mockMvc.perform(post("/api/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","phone":"03001234567","address":"Main Market"}
                                """.formatted(supplierName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(supplierName))
                .andExpect(jsonPath("$.data.phone").value("03001234567"))
                .andExpect(jsonPath("$.data.address").value("Main Market"));

        Supplier saved = supplierRepository.findAllByOrderByNameAsc().stream()
                .filter(supplier -> supplier.getName().equals(supplierName))
                .findFirst()
                .orElseThrow();
        createdSupplierId = saved.getId();

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "supplier-staff", roles = "STAFF")
    void staffCannotCreateOrListSuppliers() throws Exception {
        mockMvc.perform(post("/api/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Staff Must Not Create Supplier"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isForbidden());

        assertEquals(0, supplierRepository.findAllByOrderByNameAsc().stream()
                .filter(supplier -> supplier.getName()
                        .equals("Staff Must Not Create Supplier"))
                .count());
    }

    @Test
    @WithMockUser(username = "supplier-admin", roles = "ADMIN")
    void rejectsMissingOrBlankSupplierName() throws Exception {
        mockMvc.perform(post("/api/suppliers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","phone":"03001234567"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }
}
