package com.power_puff.bms.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * One immutable, ordered schema change for the local SQLite database.
 *
 * <p>The checksum source must change whenever the migration's effective SQL
 * or data transformation changes. Applied migrations are never edited in
 * place; add a new version instead.</p>
 */
public interface VersionedSqliteMigration {

    int version();

    String description();

    String checksumSource();

    void migrate(Connection connection) throws SQLException;
}
