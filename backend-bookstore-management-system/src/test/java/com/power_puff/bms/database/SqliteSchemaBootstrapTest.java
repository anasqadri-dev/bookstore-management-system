package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSchemaBootstrapTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void cleanDatabaseGetsElevenDocumentedTablesAndVersionHistory() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve("fresh-bookstore.db"));

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
        }

        List<VersionedSqliteMigration> migrations = List.of(
                new SalesActivityLogForeignKeyMigration(),
                new com.power_puff.bms.schoolorder.config.SchoolOrderForeignKeyMigration(),
                new com.power_puff.bms.purchase.config.PurchaseOrderForeignKeyMigration());

        new VersionedSqliteMigrationRunner(dataSource, migrations).runMigrations();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet tables = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name NOT LIKE 'sqlite_%'
                       AND name <> 'schema_migrations'
                     """)) {
            assertTrue(tables.next());
            assertEquals(11, tables.getInt(1));
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet history = statement.executeQuery(
                     "SELECT version, checksum FROM schema_migrations ORDER BY version")) {
            for (int expectedVersion = 1; expectedVersion <= 3; expectedVersion++) {
                assertTrue(history.next());
                assertEquals(expectedVersion, history.getInt("version"));
                assertEquals(64, history.getString("checksum").length());
            }
            assertTrue(!history.next());
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement, "PRAGMA foreign_keys"));
            assertEquals(0, scalar(statement, "SELECT COUNT(*) FROM pragma_foreign_key_check"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM pragma_table_info('purchase_orders') "
                            + "WHERE name = 'received_date'"));
        }
    }

    private int scalar(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
