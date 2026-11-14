package com.power_puff.bms.security;

import com.jayway.jsonpath.JsonPath;
import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=4")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProvisioningIntegrationTest {

    @Autowired
    private AdminProvisioningService provisioningService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearProvisioningState() {
        jdbcTemplate.update("DELETE FROM activity_logs");
        jdbcTemplate.update("DELETE FROM admin_bootstrap_state");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_initial_admin_insert");
    }

    @AfterEach
    void removeFailureTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_initial_admin_insert");
    }

    @Test
    void freshDatabaseOffersSetupAndCreatesOneEncodedAdmin() {
        assertTrue(provisioningService.isSetupRequired());

        User admin = provisioningService.provisionInitialAdmin(
                " Owner ", "StrongOwnerPassword123", " Store Owner ");

        assertEquals("owner", admin.getUsername());
        assertEquals("Store Owner", admin.getFullName());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(passwordEncoder.matches(
                "StrongOwnerPassword123", admin.getPasswordHash()));
        assertEquals(1, userRepository.count());
        assertEquals(1, countBootstrapMarkers());
        assertFalse(provisioningService.isSetupRequired());
    }

    @Test
    void existingDatabaseCannotBeReinitialized() {
        userRepository.saveAndFlush(new User("existing", passwordEncoder.encode("ExistingPass123"),
                "Existing Owner", UserRole.ADMIN));

        assertFalse(provisioningService.isSetupRequired());
        assertThrows(DuplicateResourceException.class,
                () -> provisioningService.provisionInitialAdmin(
                        "another", "AnotherStrongPass123", "Another Owner"));
        assertEquals(1, userRepository.count());
        assertEquals(0, countBootstrapMarkers());
    }

    @Test
    void repeatedSetupRequestCannotCreateAnotherAdmin() {
        provisioningService.provisionInitialAdmin(
                "owner", "StrongOwnerPassword123", "Store Owner");

        assertThrows(DuplicateResourceException.class,
                () -> provisioningService.provisionInitialAdmin(
                        "second", "AnotherStrongPass123", "Second Owner"));
        assertEquals(1, userRepository.count());
        assertEquals(1, countBootstrapMarkers());
    }

    @Test
    void weakOrMissingCredentialsDoNotClaimTheSetupMarker() {
        assertThrows(IllegalArgumentException.class,
                () -> provisioningService.provisionInitialAdmin(
                        "owner", "short", "Store Owner"));
        assertThrows(IllegalArgumentException.class,
                () -> provisioningService.provisionInitialAdmin(
                        " ", "StrongOwnerPassword123", "Store Owner"));
        assertThrows(IllegalArgumentException.class,
                () -> provisioningService.provisionInitialAdmin(
                        "owner", null, "Store Owner"));

        assertEquals(0, userRepository.count());
        assertEquals(0, countBootstrapMarkers());
        assertTrue(provisioningService.isSetupRequired());
    }

    @Test
    void concurrentRequestsCanCommitOnlyOneInitialAdmin() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> provisionAfterBarrier(
                    ready, start, "owner-one", "OwnerOnePassword123"));
            Future<Boolean> second = executor.submit(() -> provisionAfterBarrier(
                    ready, start, "owner-two", "OwnerTwoPassword123"));

            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();

            int successful = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successful);
            assertEquals(1, userRepository.count());
            assertEquals(1, countBootstrapMarkers());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedAccountInsertRollsBackMarkerAndAllowsRecovery() {
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_initial_admin_insert
                BEFORE INSERT ON users
                BEGIN
                    SELECT RAISE(ABORT, 'simulated interrupted provisioning');
                END
                """);

        assertThrows(RuntimeException.class,
                () -> provisioningService.provisionInitialAdmin(
                        "owner", "StrongOwnerPassword123", "Store Owner"));

        assertEquals(0, userRepository.count());
        assertEquals(0, countBootstrapMarkers());
        assertTrue(provisioningService.isSetupRequired());

        jdbcTemplate.execute("DROP TRIGGER fail_initial_admin_insert");
        User recovered = provisioningService.provisionInitialAdmin(
                "owner", "StrongOwnerPassword123", "Store Owner");

        assertEquals("owner", recovered.getUsername());
        assertEquals(1, userRepository.count());
        assertEquals(1, countBootstrapMarkers());
    }

    @Test
    void publicSetupEndpointRequiresCsrfAndBecomesUnavailableAfterProvisioning()
            throws Exception {
        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupRequired").value(true));

        String requestBody = """
                {"username":"owner","password":"StrongOwnerPassword123","fullName":"Store Owner"}
                """;

        mockMvc.perform(post("/api/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        MvcResult tokenResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        assertNotNull(session);
        String token = JsonPath.read(
                tokenResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/setup")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupRequired").value(false));

        MvcResult secondTokenResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        HttpSession secondSession = secondTokenResult.getRequest().getSession(false);
        String secondToken = JsonPath.read(
                secondTokenResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/setup")
                        .session((MockHttpSession) secondSession)
                        .header("X-CSRF-TOKEN", secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"another","password":"AnotherStrongPass123","fullName":"Another Owner"}
                                """))
                .andExpect(status().isConflict());

        assertEquals(1, userRepository.count());
    }

    @Test
    void setupEndpointRejectsMissingAndWeakCredentials() throws Exception {
        MvcResult tokenResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        String token = JsonPath.read(
                tokenResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/setup")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"owner","password":"short","fullName":"Store Owner"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
        assertEquals(0, countBootstrapMarkers());
    }

    private boolean provisionAfterBarrier(CountDownLatch ready,
                                          CountDownLatch start,
                                          String username,
                                          String password) throws Exception {
        ready.countDown();
        start.await();
        try {
            provisioningService.provisionInitialAdmin(username, password, "Store Owner");
            return true;
        } catch (DuplicateResourceException alreadyInitialized) {
            return false;
        }
    }

    private int countBootstrapMarkers() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_bootstrap_state WHERE id = 1", Integer.class);
    }
}
