package com.power_puff.bms.backup.service;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

@Service
public class DatabaseBackupService {

    private final DataSource dataSource;
    private final BackupPathValidator pathValidator;

    public DatabaseBackupService(DataSource dataSource, BackupPathValidator pathValidator) {
        this.dataSource = dataSource;
        this.pathValidator = pathValidator;
    }

    public DatabaseBackupResult createBackup(String requestedDestination) {
        Path destination = null;
        try (Connection connection = dataSource.getConnection()) {
            Path activeDatabase = findActiveDatabasePath(connection);
            destination = pathValidator.validateBackupDestination(
                    requestedDestination, activeDatabase);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("backup to " + sqlStringLiteral(destination));
            }

            verifyBackup(destination);
            return new DatabaseBackupResult(
                    destination.getFileName().toString(),
                    destination.toString(),
                    Files.size(destination),
                    Instant.now());
        } catch (IllegalArgumentException exception) {
            deleteIncompleteBackup(destination);
            throw exception;
        } catch (SQLException | IOException exception) {
            deleteIncompleteBackup(destination);
            throw new IllegalStateException(
                    "The SQLite database backup could not be completed.", exception);
        }
    }

    private Path findActiveDatabasePath(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA database_list")) {
            while (resultSet.next()) {
                if ("main".equalsIgnoreCase(resultSet.getString("name"))) {
                    String fileName = resultSet.getString("file");
                    if (fileName == null || fileName.isBlank()) {
                        throw new IllegalStateException(
                                "Database backup requires a file-backed SQLite database.");
                    }
                    return Path.of(fileName).toAbsolutePath().normalize();
                }
            }
        }
        throw new IllegalStateException("The active SQLite database could not be located.");
    }

    private void verifyBackup(Path backupPath) throws SQLException {
        String jdbcUrl = "jdbc:sqlite:" + backupPath;
        try (Connection backupConnection =
                     java.sql.DriverManager.getConnection(jdbcUrl);
             Statement statement = backupConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA integrity_check")) {
            if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                throw new SQLException("SQLite integrity_check failed for the generated backup.");
            }
        }
    }

    private String sqlStringLiteral(Path path) {
        return "'" + path.toString().replace("'", "''") + "'";
    }

    private void deleteIncompleteBackup(Path destination) {
        if (destination == null) {
            return;
        }
        try {
            Files.deleteIfExists(destination);
        } catch (IOException ignored) {
            // Preserve the original backup failure; cleanup is best-effort.
        }
    }
}
