package com.power_puff.bms.security;

import com.jayway.jsonpath.JsonPath;
import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSessionRevocationIntegrationTest {

    private static final String ADMIN_PASSWORD = "OwnerPass123";
    private static final String STAFF_PASSWORD = "StaffPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseBackupService backupService;

    @TempDir
    Path temporaryDirectory;

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long userId : createdUserIds) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
    }

    @Test
    void deactivatingStaffAccountRevokesItsAlreadyAuthenticatedSession() throws Exception {
        User admin = createUser(UserRole.ADMIN, "revoke-admin", ADMIN_PASSWORD);
        User staff = createUser(UserRole.STAFF, "revoke-staff", STAFF_PASSWORD);

        MockHttpSession adminSession = login(admin.getUsername(), ADMIN_PASSWORD);
        MockHttpSession staffSession = login(staff.getUsername(), STAFF_PASSWORD);

        mockMvc.perform(get("/api/products").session(staffSession))
                .andExpect(status().isOk());

        String csrfToken = fetchCsrfToken(adminSession);
        mockMvc.perform(put("/api/users/{id}", staff.getId())
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Revoked Staff",
                                  "newPassword":null,
                                  "active":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/auth/me").session(staffSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/products").session(staffSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingStaffPasswordRevokesOldSessionAndAllowsNewPasswordLogin()
            throws Exception {
        User admin = createUser(UserRole.ADMIN, "password-admin", ADMIN_PASSWORD);
        User staff = createUser(UserRole.STAFF, "password-staff", STAFF_PASSWORD);

        MockHttpSession adminSession = login(admin.getUsername(), ADMIN_PASSWORD);
        MockHttpSession oldStaffSession = login(staff.getUsername(), STAFF_PASSWORD);

        String csrfToken = fetchCsrfToken(adminSession);
        mockMvc.perform(put("/api/users/{id}", staff.getId())
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Password Changed Staff",
                                  "newPassword":"NewStaffPass456",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/auth/me").session(oldStaffSession))
                .andExpect(status().isUnauthorized());

        MockHttpSession newStaffSession = login(staff.getUsername(), "NewStaffPass456");
        assertNotNull(newStaffSession);
        mockMvc.perform(get("/api/auth/me").session(newStaffSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(staff.getUsername()));
    }

    @Test
    void successfulDatabaseRestoreInvalidatesPreviouslyAuthenticatedSessions()
            throws Exception {
        User admin = createUser(UserRole.ADMIN, "restore-admin", ADMIN_PASSWORD);
        Path backupPath = temporaryDirectory.resolve("session-restore.db");
        DatabaseBackupResult backup = backupService.createBackup(backupPath.toString());
        MockHttpSession adminSession = login(admin.getUsername(), ADMIN_PASSWORD);
        String csrfToken = fetchCsrfToken(adminSession);

        mockMvc.perform(post("/api/backup/restore")
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":"%s"}
                                """.formatted(jsonEscape(backup.filePath()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/auth/me").session(adminSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/products").session(adminSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failedDatabaseRestoreDoesNotInvalidateTheCurrentSession() throws Exception {
        User admin = createUser(UserRole.ADMIN, "restore-failure-admin", ADMIN_PASSWORD);
        MockHttpSession adminSession = login(admin.getUsername(), ADMIN_PASSWORD);
        String csrfToken = fetchCsrfToken(adminSession);
        Path invalidBackup = Files.createFile(
                temporaryDirectory.resolve("invalid-session-restore.db"));

        mockMvc.perform(post("/api/backup/restore")
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"backupPath":"%s"}
                                """.formatted(jsonEscape(invalidBackup.toString()))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/auth/me").session(adminSession))
                .andExpect(status().isOk());
    }

    private User createUser(UserRole role, String prefix, String password) {
        String username = prefix + "-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(new User(
                username,
                passwordEncoder.encode(password),
                "Session Test " + prefix,
                role));
        createdUserIds.add(user.getId());
        return user;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private String fetchCsrfToken(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }
    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
