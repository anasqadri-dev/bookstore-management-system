package com.power_puff.bms.backup.service;

import com.power_puff.bms.backup.dto.DatabaseRestoreResult;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class DatabaseRestoreService {

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "users",
            "products",
            "sales",
            "sale_items",
            "schools",
            "school_orders",
            "school_order_items",
            "suppliers",
            "purchase_orders",
            "purchase_order_items",
            "activity_logs"
    );

    private static final Set<String> MIGRATION_HISTORY_COLUMNS = Set.of(
            "version", "description", "checksum", "applied_at");

    private final DataSource dataSource;
    private final BackupPathValidator pathValidator;

    public DatabaseRestoreService(DataSource dataSource, BackupPathValidator pathValidator) {
        this.dataSource = dataSource;
        this.pathValidator = pathValidator;
    }

    public DatabaseRestoreResult restore(String requestedBackupPath) {
        Path safetyBackup = null;
        Path safetyDirectory = null;
        boolean rollbackRequired = false;
        boolean rollbackSucceeded = false;

        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getAutoCommit()) {
                throw new IllegalStateException(
                        "Database restore cannot run inside an active transaction.");
            }

            Path activeDatabase = findActiveDatabasePath(connection);
            Path backupPath = pathValidator.validateBackupSource(
                    requestedBackupPath, activeDatabase);

            BackupSnapshot snapshot = inspectBackup(backupPath);
            safetyDirectory = Files.createTempDirectory(
                    activeDatabase.getParent(), ".bms-restore-");
            safetyBackup = safetyDirectory.resolve("pre-restore.db");

            createOnlineBackup(connection, safetyBackup);
            verifyDatabase(safetyBackup);

            rollbackRequired = true;
            try {
                executeRestore(connection, backupPath);
                verifyConnectionDatabase(connection);
                rollbackRequired = false;

                return new DatabaseRestoreResult(
                        backupPath.getFileName().toString(),
                        snapshot.productCount(),
                        snapshot.saleCount(),
                        Instant.now());
            } catch (SQLException | RuntimeException restoreFailure) {
                try {
                    executeRestore(connection, safetyBackup);
                    verifyConnectionDatabase(connection);
                    rollbackSucceeded = true;
                } catch (SQLException | RuntimeException rollbackFailure) {
                    restoreFailure.addSuppressed(rollbackFailure);
                    throw new IllegalStateException(
                            "Database restore failed and automatic rollback could not be verified. "
                                    + "The recovery copy was preserved at " + safetyBackup,
                            restoreFailure);
                }
                throw new IllegalStateException(
                        "Database restore failed. The original database was restored from its safety copy.",
                        restoreFailure);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException(
                    "The database restore could not be completed.", exception);
        } finally {
            if (!rollbackRequired || rollbackSucceeded) {
                deleteSafetyFiles(safetyBackup, safetyDirectory);
            }
        }
    }

    private BackupSnapshot inspectBackup(Path backupPath) throws SQLException {
        try (Connection connection =
                     DriverManager.getConnection("jdbc:sqlite:" + backupPath)) {
            verifyConnectionDatabase(connection);
            return new BackupSnapshot(
                    countRows(connection, "products"),
                    countRows(connection, "sales"));
        }
    }

    private void verifyDatabase(Path databasePath) throws SQLException {
        try (Connection connection =
                     DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            verifyConnectionDatabase(connection);
        }
    }

    private void verifyConnectionDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA integrity_check")) {
            if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                throw new SQLException("SQLite integrity_check failed.");
            }
        }

        Set<String> actualTables = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT name
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name NOT LIKE 'sqlite_%'
                     """)) {
            while (resultSet.next()) {
                actualTables.add(resultSet.getString("name"));
            }
        }

        // Older valid backups predate schema versioning. New backups also
        // contain the internal migration-history table; both formats are valid.
        Set<String> expectedTablesWithHistory = new HashSet<>(REQUIRED_TABLES);
        expectedTablesWithHistory.add("schema_migrations");
        if (!REQUIRED_TABLES.equals(actualTables)
                && !expectedTablesWithHistory.equals(actualTables)) {
            throw new SQLException(
                    "Backup schema does not match the supported Bookstore database. "
                            + "Expected business tables " + REQUIRED_TABLES
                            + " with optional schema_migrations metadata, but found "
                            + actualTables + ".");
        }

        if (actualTables.contains("schema_migrations")) {
            Set<String> actualHistoryColumns = new HashSet<>();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "PRAGMA table_info('schema_migrations')")) {
                while (resultSet.next()) {
                    actualHistoryColumns.add(resultSet.getString("name"));
                }
            }
            if (!MIGRATION_HISTORY_COLUMNS.equals(actualHistoryColumns)) {
                throw new SQLException(
                        "Backup contains an unsupported schema migration history table.");
            }
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check")) {
            if (resultSet.next()) {
                throw new SQLException(
                        "Backup contains a foreign-key violation in table "
                                + resultSet.getString("table") + ".");
            }
        }
    }

    private long countRows(Connection connection, String tableName) throws SQLException {
        if (!"products".equals(tableName) && !"sales".equals(tableName)) {
            throw new IllegalArgumentException("Unsupported restore verification table.");
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + tableName)) {
            if (!resultSet.next()) {
                throw new SQLException("Could not count rows in " + tableName + ".");
            }
            return resultSet.getLong(1);
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
                                "Restore requires a file-backed SQLite database.");
                    }
                    return Path.of(fileName).toAbsolutePath().normalize();
                }
            }
        }
        throw new IllegalStateException("The active SQLite database could not be located.");
    }

    private void createOnlineBackup(Connection connection, Path destination)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("backup to " + sqlStringLiteral(destination));
        }
    }

    private void executeRestore(Connection connection, Path source) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("restore from " + sqlStringLiteral(source));
        }
    }

    private String sqlStringLiteral(Path path) {
        return "'" + path.toString().replace("'", "''") + "'";
    }

    private void deleteSafetyFiles(Path safetyBackup, Path safetyDirectory) {
        try {
            if (safetyBackup != null) {
                Files.deleteIfExists(safetyBackup);
            }
            if (safetyDirectory != null) {
                Files.deleteIfExists(safetyDirectory);
            }
        } catch (IOException ignored) {
            // A leftover safety copy is preferable to masking the restore result.
        }
    }

    private record BackupSnapshot(long productCount, long saleCount) {
    }
}
