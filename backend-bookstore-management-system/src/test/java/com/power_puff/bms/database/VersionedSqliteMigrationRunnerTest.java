package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedSqliteMigrationRunnerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void appliesMigrationsInVersionOrderAndSkipsThemOnSubsequentRuns() throws Exception {
        SQLiteDataSource dataSource = dataSource("ordered-migrations.db");
        StringBuilder executionOrder = new StringBuilder();
        AtomicInteger firstRuns = new AtomicInteger();
        AtomicInteger secondRuns = new AtomicInteger();

        VersionedSqliteMigration second = migration(
                2, "second migration", "stable-v2", executionOrder, secondRuns);
        VersionedSqliteMigration first = migration(
                1, "first migration", "stable-v1", executionOrder, firstRuns);

        VersionedSqliteMigrationRunner runner =
                new VersionedSqliteMigrationRunner(dataSource, List.of(second, first));

        runner.runMigrations();
        runner.runMigrations();

        assertEquals("12", executionOrder.toString());
        assertEquals(1, firstRuns.get());
        assertEquals(1, secondRuns.get());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT version, description, checksum FROM schema_migrations ORDER BY version")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt("version"));
            assertEquals("first migration", rows.getString("description"));
            assertEquals(64, rows.getString("checksum").length());

            assertTrue(rows.next());
            assertEquals(2, rows.getInt("version"));
            assertEquals("second migration", rows.getString("description"));
            assertEquals(64, rows.getString("checksum").length());

            assertTrue(!rows.next());
        }
    }

    @Test
    void refusesToRunWhenAnAppliedMigrationChecksumChanges() {
        SQLiteDataSource dataSource = dataSource("checksum-mismatch.db");
        AtomicInteger runs = new AtomicInteger();
        StringBuilder executionOrder = new StringBuilder();

        VersionedSqliteMigration original = migration(
                1, "first migration", "original-definition", executionOrder, runs);
        new VersionedSqliteMigrationRunner(dataSource, List.of(original)).runMigrations();

        VersionedSqliteMigration modified = migration(
                1, "first migration", "modified-definition", executionOrder, runs);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new VersionedSqliteMigrationRunner(
                        dataSource, List.of(modified)).runMigrations());

        assertTrue(exception.getMessage().contains("changed after it was applied"));
        assertEquals(1, runs.get());
    }

    @Test
    void rollsBackSchemaChangesAndDoesNotRecordFailedMigration() throws Exception {
        SQLiteDataSource dataSource = dataSource("failed-migration.db");
        VersionedSqliteMigration failing = new VersionedSqliteMigration() {
            @Override
            public int version() {
                return 1;
            }

            @Override
            public String description() {
                return "migration which fails";
            }

            @Override
            public String checksumSource() {
                return "failing-migration-v1";
            }

            @Override
            public void migrate(Connection connection) throws SQLException {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE temporary_migration_table (id INTEGER PRIMARY KEY)");
                }
                throw new SQLException("simulated migration failure");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> new VersionedSqliteMigrationRunner(
                        dataSource, List.of(failing)).runMigrations());

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master "
                             + "WHERE type = 'table' AND name = 'temporary_migration_table'")) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT COUNT(*) FROM schema_migrations")) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }
    }

    @Test
    void rejectsMigrationRegistryWithVersionGaps() {
        SQLiteDataSource dataSource = dataSource("migration-gap.db");
        AtomicInteger runs = new AtomicInteger();
        StringBuilder executionOrder = new StringBuilder();

        VersionedSqliteMigration onlySecond = migration(
                2, "second migration", "stable-v2", executionOrder, runs);

        assertThrows(IllegalStateException.class,
                () -> new VersionedSqliteMigrationRunner(
                        dataSource, List.of(onlySecond)).runMigrations());
        assertEquals(0, runs.get());
    }

    private SQLiteDataSource dataSource(String fileName) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve(fileName));
        return dataSource;
    }

    private VersionedSqliteMigration migration(
            int version,
            String description,
            String checksumSource,
            StringBuilder executionOrder,
            AtomicInteger runCount) {
        return new VersionedSqliteMigration() {
            @Override
            public int version() {
                return version;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String checksumSource() {
                return checksumSource;
            }

            @Override
            public void migrate(Connection connection) throws SQLException {
                executionOrder.append(version);
                runCount.incrementAndGet();
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE migration_v" + version
                            + " (id INTEGER PRIMARY KEY)");
                }
            }
        };
    }
}
