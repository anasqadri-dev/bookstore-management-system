package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFailureAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> attemptedUsernames = new ArrayList<>();
    private Long createdUserId;

    @AfterEach
    void cleanUp() {
        for (String username : attemptedUsernames) {
            jdbcTemplate.update(
                    "DELETE FROM activity_logs WHERE action = 'LOGIN_FAILED' AND details LIKE ?",
                    "%username='" + username + "'%");
        }
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void recordsUnknownUsernameAndWrongPasswordWithoutCredentialsOrFakeActor()
            throws Exception {
        String unknownUsername = "unknown-" + UUID.randomUUID();
        attemptedUsernames.add(unknownUsername);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"UnknownPass123"}
                                """.formatted(unknownUsername)))
                .andExpect(status().isUnauthorized());

        String existingUsername = "known-" + UUID.randomUUID();
        attemptedUsernames.add(existingUsername);
        User user = userRepository.saveAndFlush(new User(
                existingUsername, passwordEncoder.encode("CorrectPass123"),
                "Audit Test User", UserRole.STAFF));
        createdUserId = user.getId();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"WrongSecret987"}
                                """.formatted(existingUsername)))
                .andExpect(status().isUnauthorized());

        for (String username : attemptedUsernames) {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM activity_logs
                    WHERE action = 'LOGIN_FAILED'
                      AND user_id IS NULL
                      AND details = ?
                    """, Integer.class, "Failed login for username='" + username + "'");
            assertEquals(1, count);
        }

        Integer leakedPasswordCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM activity_logs
                WHERE action = 'LOGIN_FAILED'
                  AND details LIKE '%WrongSecret987%'
                """, Integer.class);
        assertEquals(0, leakedPasswordCount);
    }
}
