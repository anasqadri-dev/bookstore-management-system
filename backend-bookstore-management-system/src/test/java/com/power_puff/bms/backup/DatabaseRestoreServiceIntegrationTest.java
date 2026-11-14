package com.power_puff.bms.backup;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.dto.DatabaseRestoreResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import com.power_puff.bms.backup.service.DatabaseRestoreService;
import com.power_puff.bms.security.AdminProvisioningService;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseRestoreServiceIntegrationTest {

    @Autowired
    private DatabaseBackupService backupService;

    @Autowired
    private DatabaseRestoreService restoreService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminProvisioningService adminProvisioningService;

    @TempDir
    Path temporaryDirectory;

    @Test
    void restoresSavedDatabaseSnapshotAndReportsRestoredCounts() throws Exception {
        String existingSupplier = "Restore Existing " + UUID.randomUUID();
        String laterSupplier = "Restore Later " + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", existingSupplier);
        Map<String, Long> expectedTableCounts = currentBusinessTableCounts();

        Path backupPath = temporaryDirectory.resolve("before-change.db");
        DatabaseBackupResult backup = backupService.createBackup(backupPath.toString());

        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", laterSupplier);
        assertEquals(1, countSupplier(laterSupplier));

        DatabaseRestoreResult restored = restoreService.restore(backup.filePath());

        assertEquals(0, countSupplier(laterSupplier));
        assertEquals(1, countSupplier(existingSupplier));
        assertEquals(expectedTableCounts, restored.tableCounts(),
                "Restore must preserve row counts for every business table");
        assertTrue(restored.productCount() >= 0);
        assertTrue(restored.saleCount() >= 0);
        assertEquals("before-change.db", restored.fileName());
        assertNotNull(restored.restoredAt());

        jdbcTemplate.update("DELETE FROM suppliers WHERE name = ?", existingSupplier);
    }

    @Test
    void restoresLegacyBackupAndRecreatesTheInternalFirstRunMarker() throws Exception {
        userRepository.saveAndFlush(new User(
                "restore-owner", passwordEncoder.encode("RestoreOwnerPass123"),
                "Restore Owner", UserRole.ADMIN));

        Path backupPath = temporaryDirectory.resolve("legacy-backup.db");
        DatabaseBackupResult backup = backupService.createBackup(backupPath.toString());

        // Simulate a valid backup produced before the first-run marker existed.
        try (var connection = java.sql.DriverManager.getConnection(
                     "jdbc:sqlite:" + backupPath);
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE admin_bootstrap_state");
        }

        restoreService.restore(backup.filePath());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master "
                        + "WHERE type = 'table' AND name = 'admin_bootstrap_state'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, "restore-owner"));
        assertTrue(!adminProvisioningService.isSetupRequired());

        jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id IN "
                + "(SELECT id FROM users WHERE username = ?)", "restore-owner");
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", "restore-owner");
    }

    @Test
    void rejectsEmptyBackupAndLeavesCurrentDatabaseUnchanged() throws Exception {
        String marker = "Restore Must Preserve " + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", marker);
        Path emptyBackup = Files.createFile(temporaryDirectory.resolve("empty-backup.db"));

        assertThrows(IllegalArgumentException.class,
                () -> restoreService.restore(emptyBackup.toString()));

        assertEquals(1, countSupplier(marker));
        jdbcTemplate.update("DELETE FROM suppliers WHERE name = ?", marker);
    }

    @Test
    void rejectsDatabaseWithoutTheExpectedElevenTables() throws Exception {
        Path invalidBackup = temporaryDirectory.resolve("wrong-schema.db");
        try (var connection = java.sql.DriverManager.getConnection(
                     "jdbc:sqlite:" + invalidBackup);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE unrelated (id INTEGER PRIMARY KEY)");
        }

        String marker = "Schema Validation " + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", marker);

        assertThrows(IllegalStateException.class,
                () -> restoreService.restore(invalidBackup.toString()));

        assertEquals(1, countSupplier(marker));
        jdbcTemplate.update("DELETE FROM suppliers WHERE name = ?", marker);
    }

    private Map<String, Long> currentBusinessTableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : java.util.List.of(
                "users", "products", "sales", "sale_items", "schools",
                "school_orders", "school_order_items", "suppliers",
                "purchase_orders", "purchase_order_items", "activity_logs")) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table, Long.class);
            counts.put(table, count);
        }
        return Map.copyOf(counts);
    }

    private int countSupplier(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM suppliers WHERE name = ?",
                Integer.class,
                name);
    }
}
