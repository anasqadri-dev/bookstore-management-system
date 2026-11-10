package com.power_puff.bms.schoolorder.controller;

import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolOrderStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolOrderRepository schoolOrderRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    private Integer schoolOrderId;
    private Integer schoolId;
    private Long userId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Status School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "status-admin-" + suffix, "test-hash",
                "Status Admin", UserRole.ADMIN));
        userId = user.getId();

        SchoolOrder order = schoolOrderRepository.saveAndFlush(
                new SchoolOrder(school, user));
        schoolOrderId = order.getId();
    }

    @AfterEach
    void cleanUp() {
        if (schoolOrderId != null) {
            schoolOrderRepository.deleteById(schoolOrderId);
        }
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    @WithMockUser(username = "school-order-admin", roles = "ADMIN")
    void adminCanSetEachStatusDefinedByTheSds() throws Exception {
        for (SchoolOrderStatus status : SchoolOrderStatus.values()) {
            mockMvc.perform(patch("/api/school-orders/{id}/status", schoolOrderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status":"%s"}
                                    """.formatted(status.name())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value(status.name()));

            assertEquals(status, schoolOrderRepository.findById(schoolOrderId)
                    .orElseThrow().getStatus());
        }

        mockMvc.perform(get("/api/school-orders/{id}", schoolOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser(username = "school-order-staff", roles = "STAFF")
    void staffCannotChangeSchoolOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/school-orders/{id}/status", schoolOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isForbidden());

        assertEquals(SchoolOrderStatus.PENDING,
                schoolOrderRepository.findById(schoolOrderId).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "school-order-admin", roles = "ADMIN")
    void rejectsMissingOrUnsupportedStatusValues() throws Exception {
        mockMvc.perform(patch("/api/school-orders/{id}/status", schoolOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/school-orders/{id}/status", schoolOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(SchoolOrderStatus.PENDING,
                schoolOrderRepository.findById(schoolOrderId).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "school-order-admin", roles = "ADMIN")
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(patch("/api/school-orders/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isNotFound());
    }
}
