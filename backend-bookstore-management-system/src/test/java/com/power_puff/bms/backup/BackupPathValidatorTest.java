package com.power_puff.bms.backup;

import com.power_puff.bms.backup.service.BackupPathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupPathValidatorTest {

    private final BackupPathValidator validator = new BackupPathValidator();

    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsNewAbsoluteDatabaseBackupPath() {
        Path destination = temporaryDirectory.resolve("backup.db");

        assertEquals(destination,
                validator.validateBackupDestination(destination.toString(), null));
    }

    @Test
    void rejectsRelativeBackupPath() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateBackupDestination("backup.db", null));
    }

    @Test
    void rejectsUnsupportedBackupExtension() {
        Path destination = temporaryDirectory.resolve("backup.txt");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateBackupDestination(destination.toString(), null));
    }

    @Test
    void rejectsExistingBackupDestination() throws Exception {
        Path destination = Files.createFile(temporaryDirectory.resolve("backup.db"));

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateBackupDestination(destination.toString(), null));
    }

    @Test
    void rejectsActiveDatabaseAsBackupDestination() throws Exception {
        Path activeDatabase = Files.createFile(temporaryDirectory.resolve("bookstore.db"));

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateBackupDestination(activeDatabase.toString(), activeDatabase));
    }

    @Test
    void rejectsMissingDestinationDirectory() {
        Path destination = temporaryDirectory.resolve("missing/backup.db");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateBackupDestination(destination.toString(), null));
    }
}
