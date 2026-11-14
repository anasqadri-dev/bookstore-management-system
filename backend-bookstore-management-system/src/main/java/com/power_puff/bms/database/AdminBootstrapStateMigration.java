package com.power_puff.bms.database;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Persists the one-time first-admin provisioning marker.
 *
 * <p>The marker is inserted in the same transaction as the first Admin
 * account. A failed provisioning transaction therefore leaves setup
 * available for a safe retry.</p>
 */
@Component
public class AdminBootstrapStateMigration implements VersionedSqliteMigration {

    @Override
    public int version() {
        return 5;
    }

    @Override
    public String description() {
        return "Add atomic first-admin provisioning state";
    }

    @Override
    public String checksumSource() {
        return "v5|admin_bootstrap_state|singleton-id-1|completed-at-required|transactional-claim";
    }

    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS admin_bootstrap_state (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        completed_at TIMESTAMP NOT NULL
                    )
                    """);
        }
    }
}
