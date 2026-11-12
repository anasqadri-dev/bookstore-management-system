package com.power_puff.bms.database;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Applies SQLite schema migrations in version order and stores each applied
 * version, description and SHA-256 checksum in schema_migrations.
 *
 * <p>Each migration and its history row commit in the same transaction.
 * SQLite foreign-key enforcement is disabled only on this migration
 * connection while a table-rebuild migration runs, then restored before
 * application startup completes.</p>
 */
@Component
@Order(0)
public class VersionedSqliteMigrationRunner implements ApplicationRunner {

    private static final String HISTORY_TABLE = "schema_migrations";

    private final DataSource dataSource;
    private final List<VersionedSqliteMigration> migrations;

    public VersionedSqliteMigrationRunner(
            DataSource dataSource,
            List<VersionedSqliteMigration> migrations) {
        this.dataSource = dataSource;
        this.migrations = List.copyOf(migrations);
    }

    @Override
    public void run(ApplicationArguments args) {
        runMigrations();
    }

    /**
     * Public to allow focused tests against isolated, file-backed SQLite stores.
     */
    public void runMigrations() {
        List<VersionedSqliteMigration> ordered = new ArrayList<>(migrations);
        ordered.sort(Comparator.comparingInt(VersionedSqliteMigration::version));
        validateRegistry(ordered);

        try (Connection connection = dataSource.getConnection()) {
            ensureHistoryTable(connection);

            int latestKnownVersion = readLatestAppliedVersion(connection);
            int latestCodeVersion = ordered.isEmpty()
                    ? 0 : ordered.getLast().version();
            if (latestKnownVersion > latestCodeVersion) {
                throw new IllegalStateException(
                        "The database schema is newer than this application build. "
                                + "Database version " + latestKnownVersion
                                + ", supported version " + latestCodeVersion + ".");
            }

            for (VersionedSqliteMigration migration : ordered) {
                applyIfNeeded(connection, migration);
            }

            enableAndVerifyForeignKeys(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "SQLite schema migration failed. The application will not start "
                            + "against a partially migrated database.",
                    exception);
        }
    }

    private void validateRegistry(List<VersionedSqliteMigration> ordered) {
        Set<Integer> versions = new HashSet<>();
        for (int index = 0; index < ordered.size(); index++) {
            VersionedSqliteMigration migration = ordered.get(index);
            if (migration.version() < 1 || !versions.add(migration.version())) {
                throw new IllegalStateException(
                        "SQLite migrations must have unique positive versions.");
            }
            int expected = index + 1;
            if (migration.version() != expected) {
                throw new IllegalStateException(
                        "SQLite migration registry has a gap. Expected version "
                                + expected + " but found " + migration.version() + ".");
            }
            if (migration.description() == null || migration.description().isBlank()
                    || migration.checksumSource() == null
                    || migration.checksumSource().isBlank()) {
                throw new IllegalStateException(
                        "SQLite migration " + migration.version()
                                + " must define a description and checksum source.");
            }
        }
    }

    private void ensureHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version INTEGER PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        checksum CHAR(64) NOT NULL,
                        applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    private int readLatestAppliedVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COALESCE(MAX(version), 0) FROM " + HISTORY_TABLE)) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    private void applyIfNeeded(Connection connection,
                               VersionedSqliteMigration migration) throws SQLException {
        String expectedChecksum = checksum(migration.checksumSource());
        AppliedMigration applied = findApplied(connection, migration.version());

        if (applied != null) {
            if (!applied.description().equals(migration.description())
                    || !applied.checksum().equals(expectedChecksum)) {
                throw new IllegalStateException(
                        "SQLite migration " + migration.version()
                                + " has changed after it was applied. "
                                + "Create a new migration version instead of editing "
                                + "an applied migration.");
            }
            return;
        }

        setForeignKeys(connection, false);
        connection.setAutoCommit(false);
        try {
            migration.migrate(connection);
            verifyForeignKeyIntegrity(connection);
            recordAppliedMigration(connection, migration, expectedChecksum);
            connection.commit();
        } catch (Exception exception) {
            connection.rollback();
            throw new IllegalStateException(
                    "Could not apply SQLite migration " + migration.version()
                            + " (" + migration.description() + ").",
                    exception);
        } finally {
            connection.setAutoCommit(true);
            setForeignKeys(connection, true);
        }
    }

    private AppliedMigration findApplied(Connection connection, int version)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT description, checksum FROM " + HISTORY_TABLE
                        + " WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next()
                        ? new AppliedMigration(rows.getString("description"),
                                rows.getString("checksum"))
                        : null;
            }
        }
    }

    private void recordAppliedMigration(Connection connection,
                                        VersionedSqliteMigration migration,
                                        String checksum) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + HISTORY_TABLE
                        + " (version, description, checksum) VALUES (?, ?, ?)")) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, checksum);
            statement.executeUpdate();
        }
    }

    private void setForeignKeys(Connection connection, boolean enabled) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=" + (enabled ? "ON" : "OFF"));
        }
    }

    private void enableAndVerifyForeignKeys(Connection connection) throws SQLException {
        setForeignKeys(connection, true);
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA foreign_keys")) {
            if (!rows.next() || rows.getInt(1) != 1) {
                throw new SQLException(
                        "SQLite foreign-key enforcement could not be enabled.");
            }
        }
        verifyForeignKeyIntegrity(connection);
    }

    private void verifyForeignKeyIntegrity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet violations = statement.executeQuery("PRAGMA foreign_key_check")) {
            if (violations.next()) {
                throw new SQLException(
                        "SQLite foreign-key integrity check failed for table "
                                + violations.getString("table") + ".");
            }
        }
    }

    private String checksum(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private record AppliedMigration(String description, String checksum) {
    }
}
