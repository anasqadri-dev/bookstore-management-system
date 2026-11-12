package com.power_puff.bms.backup;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.dto.DatabaseRestoreResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import com.power_puff.bms.backup.service.DatabaseRestoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @TempDir
    Path temporaryDirectory;

    @Test
    void restoresSavedDatabaseSnapshotAndReportsRestoredCounts() throws Exception {
        String existingSupplier = "Restore Existing " + UUID.randomUUID();
        String laterSupplier = "Restore Later " + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", existingSupplier);

        Path backupPath = temporaryDirectory.resolve("before-change.db");
        DatabaseBackupResult backup = backupService.createBackup(backupPath.toString());

        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", laterSupplier);
        assertEquals(1, countSupplier(laterSupplier));

        DatabaseRestoreResult restored = restoreService.restore(backup.filePath());

        assertEquals(0, countSupplier(laterSupplier));
        assertEquals(1, countSupplier(existingSupplier));
        assertTrue(restored.productCount() >= 0);
        assertTrue(restored.saleCount() >= 0);
        assertEquals("before-change.db", restored.fileName());
        assertNotNull(restored.restoredAt());

        jdbcTemplate.update("DELETE FROM suppliers WHERE name = ?", existingSupplier);
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

    private int countSupplier(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM suppliers WHERE name = ?",
                Integer.class,
                name);
    }
}
