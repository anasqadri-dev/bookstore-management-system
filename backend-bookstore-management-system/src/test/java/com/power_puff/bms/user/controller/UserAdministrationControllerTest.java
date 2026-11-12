package com.power_puff.bms.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
    }

    @Test
    @WithMockUser(username = "user-admin", roles = "ADMIN")
    void adminCanCreateAndListStaffWithoutExposingPasswordHash() throws Exception {
        String username = "ApiStaff" + UUID.randomUUID().toString().replace("-", "");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"SecurePass123",
                                  "fullName":"API Created Staff"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username")
                        .value(username.toLowerCase()))
                .andExpect(jsonPath("$.data.fullName").value("API Created Staff"))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        User saved = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        createdUserIds.add(saved.getId());
        assertTrue(passwordEncoder.matches("SecurePass123", saved.getPasswordHash()));
        assertFalse(saved.getPasswordHash().equals("SecurePass123"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.username == '%s')]".formatted(
                        username.toLowerCase())).exists())
                .andExpect(jsonPath("$.data[?(@.username == '%s')].passwordHash".formatted(
                        username.toLowerCase())).doesNotExist());
    }

    @Test
    @WithMockUser(username = "user-admin", roles = "ADMIN")
    void adminCanUpdateAndDeactivateStaffWithoutDeletingTheRecord() throws Exception {
        User staff = createStaff("update-" + UUID.randomUUID());

        mockMvc.perform(put("/api/users/{id}", staff.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Updated Staff Name",
                                  "newPassword":"ChangedPass123",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Staff Name"))
                .andExpect(jsonPath("$.data.active").value(true));

        User updated = userRepository.findById(staff.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("ChangedPass123", updated.getPasswordHash()));

        mockMvc.perform(delete("/api/users/{id}", staff.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));

        User deactivated = userRepository.findById(staff.getId()).orElseThrow();
        assertFalse(deactivated.isActive());
        assertEquals(UserRole.STAFF, deactivated.getRole());
    }

    @Test
    @WithMockUser(username = "user-admin", roles = "ADMIN")
    void rejectsDuplicateUsernameAndInvalidPasswordWithoutCreatingAnotherAccount()
            throws Exception {
        String username = "duplicate-" + UUID.randomUUID();
        User existing = createStaff(username);
        long before = userRepository.count();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"SecurePass123",
                                  "fullName":"Duplicate Account"
                                }
                                """.formatted(username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        String invalidUsername = "invalid-" + UUID.randomUUID();
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"short",
                                  "fullName":"Invalid Account"
                                }
                                """.formatted(invalidUsername)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(before, userRepository.count());
        assertTrue(userRepository.findById(existing.getId()).isPresent());
        assertTrue(userRepository.findByUsernameIgnoreCase(invalidUsername).isEmpty());
    }

    @Test
    @WithMockUser(username = "user-staff", roles = "STAFF")
    void staffCannotListCreateUpdateOrDeactivateAccounts() throws Exception {
        User target = createStaff("staff-forbidden-" + UUID.randomUUID());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"must-not-create",
                                  "password":"SecurePass123",
                                  "fullName":"Must Not Create"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/{id}", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Must Not Update","newPassword":null,"active":false}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/{id}", target.getId()))
                .andExpect(status().isForbidden());

        User unchanged = userRepository.findById(target.getId()).orElseThrow();
        assertEquals(target.getFullName(), unchanged.getFullName());
        assertTrue(unchanged.isActive());
    }

    @Test
    void unauthenticatedUsersCannotAccessUserAdministration() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"anonymous","password":"SecurePass123",
                                 "fullName":"Anonymous"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private User createStaff(String username) {
        User saved = userRepository.saveAndFlush(new User(
                username, passwordEncoder.encode("OriginalPass123"),
                "Original Staff Name", UserRole.STAFF));
        createdUserIds.add(saved.getId());
        return saved;
    }
}
