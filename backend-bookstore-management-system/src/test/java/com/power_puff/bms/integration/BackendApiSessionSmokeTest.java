package com.power_puff.bms.integration;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApiSessionSmokeTest {

    private static final String TEST_PASSWORD = "SmokePass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> testUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : testUserIds) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
    }

    @Test
    void adminCanLoginAndReachAdminAndSharedBackendEndpoints() throws Exception {
        User admin = createUser(UserRole.ADMIN, "admin");

        MockHttpSession session = login(admin);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(admin.getUsername()))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(get("/api/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/suppliers").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/reports/inventory").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/users").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/logout").with(csrf())
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void staffCanLoginAndReachSharedEndpointsButIsDeniedAdminEndpoints()
            throws Exception {
        User staff = createUser(UserRole.STAFF, "staff");
        MockHttpSession session = login(staff);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STAFF"));

        mockMvc.perform(get("/api/dashboard").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/suppliers").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/inventory").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/purchase-orders").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/school-orders").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthIsPublicButDashboardRequiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(user.getUsername(), TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private User createUser(UserRole role, String prefix) {
        String username = prefix + "-smoke-" + UUID.randomUUID();
        User saved = userRepository.saveAndFlush(new User(
                username,
                passwordEncoder.encode(TEST_PASSWORD),
                "API Smoke " + prefix,
                role));
        testUserIds.add(saved.getId());
        assertTrue(saved.isActive());
        return saved;
    }
}
