package com.power_puff.bms.schoolorder.controller;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolOrderControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolOrderRepository schoolOrderRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Integer schoolId;
    private Integer productId;
    private Integer createdOrderId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        if (createdOrderId != null) {
            schoolOrderRepository.deleteById(createdOrderId);
        }
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    @WithMockUser(username = "order-controller-admin", roles = "ADMIN")
    void adminCanCreateAndListSchoolOrders() throws Exception {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Controller School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "order-controller-admin", "test-hash",
                "Order Controller Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "ORDER-CONTROLLER-" + suffix, "Controller Order Book", null,
                new BigDecimal("20.00"), 10, 1));
        productId = product.getId();

        mockMvc.perform(post("/api/school-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolId":%d,"items":[{"productId":%d,"quantity":2}]}
                                """.formatted(schoolId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schoolId").value(schoolId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        mockMvc.perform(get("/api/school-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        var createdOrder = schoolOrderRepository.findAllBySchoolIdOrderByOrderDateDesc(
                schoolId).getFirst();
        createdOrderId = createdOrder.getId();
        assertEquals(1, schoolOrderRepository.findAllBySchoolIdOrderByOrderDateDesc(
                schoolId).size());

        mockMvc.perform(patch("/api/school-orders/{id}/status", createdOrderId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        mockMvc.perform(patch("/api/school-orders/{id}/status", createdOrderId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        mockMvc.perform(get("/api/school-orders/{id}/quotation", createdOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(40.00))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(40.00));
    }

    @Test
    @WithMockUser(username = "order-status-admin", roles = "ADMIN")
    void rejectsUnsupportedSchoolOrderStatusValuesAsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/school-orders/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "order-status-admin", roles = "ADMIN")
    void rejectsMissingSchoolOrderStatusAsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/school-orders/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "order-missing-school-admin", roles = "ADMIN")
    void rejectsOrderForUnknownSchoolAsNotFound() throws Exception {
        mockMvc.perform(post("/api/school-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolId":999999,
                                 "items":[{"productId":1,"quantity":1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "order-controller-staff", roles = "STAFF")
    void staffCannotCreateOrListSchoolOrders() throws Exception {
        mockMvc.perform(post("/api/school-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolId":1,"items":[{"productId":1,"quantity":1}]}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/school-orders"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/school-orders/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"READY"}
                                """))
                .andExpect(status().isForbidden());
    }
}
