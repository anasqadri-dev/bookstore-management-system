package com.power_puff.bms.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConnectionPoolLifecycleTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void poolShutdownRollsBackBorrowedTransactionsAndLeavesDatabaseReopenable()
            throws Exception {
        Path databaseFile = temporaryDirectory.resolve("lifecycle.db");
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:sqlite:" + databaseFile);
        configuration.setDriverClassName("org.sqlite.JDBC");
        configuration.setMaximumPoolSize(1);
        configuration.setMinimumIdle(0);
        configuration.setConnectionInitSql("PRAGMA foreign_keys=ON");

        HikariDataSource dataSource = new HikariDataSource(configuration);
        try (ConnectionScope ignored = new ConnectionScope(dataSource)) {
            try (var connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE lifecycle_records (
                            id INTEGER PRIMARY KEY,
                            value TEXT NOT NULL
                        )
                        """);

                connection.setAutoCommit(false);
                statement.executeUpdate(
                        "INSERT INTO lifecycle_records (id, value) VALUES (1, 'committed')");
                connection.commit();
            }

            // Return a connection with an uncommitted transaction to the pool.
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                statement.executeUpdate(
                        "INSERT INTO lifecycle_records (id, value) VALUES (2, 'uncommitted')");
            }

            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT COUNT(*) FROM lifecycle_records")) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }

        assertTrue(dataSource.isClosed());

        // A fresh SQLite connection can reopen the file after the pool is shut down.
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
             var statement = connection.createStatement()) {
            try (ResultSet integrity = statement.executeQuery("PRAGMA integrity_check")) {
                assertTrue(integrity.next());
                assertEquals("ok", integrity.getString(1));
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT value FROM lifecycle_records WHERE id = 1")) {
                assertTrue(resultSet.next());
                assertEquals("committed", resultSet.getString(1));
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT COUNT(*) FROM lifecycle_records WHERE id = 2")) {
                resultSet.next();
                assertEquals(0, resultSet.getInt(1));
            }

            statement.execute("BEGIN EXCLUSIVE");
            statement.executeUpdate(
                    "INSERT INTO lifecycle_records (id, value) VALUES (3, 'after-shutdown')");
            statement.execute("COMMIT");
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
             var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM lifecycle_records WHERE id = 3")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }

    private static final class ConnectionScope implements AutoCloseable {

        private final HikariDataSource dataSource;

        private ConnectionScope(HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void close() {
            dataSource.close();
        }
    }
}
