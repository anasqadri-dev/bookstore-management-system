package com.power_puff.bms.database;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Allows authentication-failure audit events to exist without attributing
 * them to a user who has not authenticated.
 */
@Component
public class NullableActivityLogActorMigration implements VersionedSqliteMigration {

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String description() {
        return "Allow anonymous authentication audit events";
    }

    @Override
    public String checksumSource() {
        return "v2|activity_logs.user_id-nullable|copy-existing-rows|preserve-fk";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (isUserIdNullable(statement)) {
                return;
            }

            statement.execute("DROP TABLE IF EXISTS activity_logs_nullable_new");
            statement.execute("""
                    CREATE TABLE activity_logs_nullable_new (
                        id INTEGER PRIMARY KEY,
                        user_id INTEGER REFERENCES users(id),
                        action VARCHAR(50) NOT NULL,
                        details VARCHAR(1000),
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO activity_logs_nullable_new
                        (id, user_id, action, details, created_at)
                    SELECT id, user_id, action, details, created_at
                    FROM activity_logs
                    """);
            statement.execute("DROP TABLE activity_logs");
            statement.execute(
                    "ALTER TABLE activity_logs_nullable_new RENAME TO activity_logs");

            try (var violations = statement.executeQuery(
                    "PRAGMA foreign_key_check('activity_logs')")) {
                if (violations.next()) {
                    throw new SQLException(
                            "Activity-log migration created a foreign-key violation.");
                }
            }
        }
    }

    private boolean isUserIdNullable(Statement statement) throws SQLException {
        try (var columns = statement.executeQuery("PRAGMA table_info('activity_logs')")) {
            while (columns.next()) {
                if ("user_id".equalsIgnoreCase(columns.getString("name"))) {
                    return columns.getInt("notnull") == 0;
                }
            }
        }
        throw new SQLException("The activity_logs.user_id column was not found.");
    }
}
