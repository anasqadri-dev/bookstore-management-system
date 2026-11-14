package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NullableActivityLogActorMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void makesActivityLogActorNullableAndPreservesExistingRows() throws Exception {
        Path database = temporaryDirectory.resolve("audit-migration.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("""
                    CREATE TABLE users (
                        id INTEGER PRIMARY KEY,
                        username TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE activity_logs (
                        id INTEGER PRIMARY KEY,
                        user_id INTEGER NOT NULL REFERENCES users(id),
                        action VARCHAR(50) NOT NULL,
                        details VARCHAR(1000),
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            statement.execute("INSERT INTO users (id, username) VALUES (1, 'existing')");
            statement.execute("""
                    INSERT INTO activity_logs (id, user_id, action, details, created_at)
                    VALUES (7, 1, 'LOGIN', 'Existing event', '2026-01-01T00:00:00Z')
                    """);

            new NullableActivityLogActorMigration().migrate(connection);

            try (var columns = statement.executeQuery("PRAGMA table_info('activity_logs')")) {
                boolean nullable = false;
                while (columns.next()) {
                    if ("user_id".equals(columns.getString("name"))) {
                        nullable = columns.getInt("notnull") == 0;
                    }
                }
                assertEquals(true, nullable);
            }

            try (var rows = statement.executeQuery(
                    "SELECT id, user_id, action, details FROM activity_logs WHERE id = 7")) {
                assertEquals(true, rows.next());
                assertEquals(1, rows.getInt("user_id"));
                assertEquals("LOGIN", rows.getString("action"));
                assertEquals("Existing event", rows.getString("details"));
            }

            statement.execute("""
                    INSERT INTO activity_logs (user_id, action, details, created_at)
                    VALUES (NULL, 'LOGIN_FAILED', 'Unknown user', '2026-01-01T00:00:01Z')
                    """);
            try (var rows = statement.executeQuery(
                    "SELECT user_id FROM activity_logs WHERE action = 'LOGIN_FAILED'")) {
                assertEquals(true, rows.next());
                assertEquals(null, rows.getObject("user_id"));
            }
        }
    }
}
