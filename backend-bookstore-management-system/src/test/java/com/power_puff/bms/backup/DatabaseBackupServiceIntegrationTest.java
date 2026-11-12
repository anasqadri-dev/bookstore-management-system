package com.power_puff.bms.backup;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseBackupServiceIntegrationTest {

    @Autowired
    private DatabaseBackupService backupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsReadableConsistentBackupOfActiveDatabase() throws Exception {
        String supplierName = "Backup Snapshot " + UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO suppliers (name) VALUES (?)", supplierName);

        Path destination = temporaryDirectory.resolve("bookstore-backup.db");
        DatabaseBackupResult result = backupService.createBackup(destination.toString());

        assertTrue(Files.isRegularFile(destination));
        assertTrue(result.sizeBytes() > 0);
        assertEquals("bookstore-backup.db", result.fileName());
        assertEquals(destination.toString(), result.filePath());

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + destination);
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM suppliers WHERE name = ?");
        ) {
            statement.setString(1, supplierName);
            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(1, resultSet.getInt(1));
            }
        } finally {
            jdbcTemplate.update("DELETE FROM suppliers WHERE name = ?", supplierName);
        }
    }

    @Test
    void refusesToOverwriteExistingBackupFile() throws Exception {
        Path destination = Files.createFile(temporaryDirectory.resolve("existing.db"));

        assertThrowsIllegalArgument(() -> backupService.createBackup(destination.toString()));
        assertEquals(0, Files.size(destination));
    }

    private void assertThrowsIllegalArgument(Runnable operation) {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, operation::run);
    }
}
