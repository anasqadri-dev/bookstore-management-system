package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesActivityLogForeignKeyMigrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void addsExpectedForeignKeysWithoutLosingExistingSalesItemsOrActivityLogs()
            throws Exception {
        Path databasePath = temporaryDirectory.resolve("legacy-bms.db");
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databasePath);

        createLegacySchemaAndRows(dataSource);

        SalesActivityLogForeignKeyMigration migration =
                new SalesActivityLogForeignKeyMigration();
        VersionedSqliteMigrationRunner runner =
                new VersionedSqliteMigrationRunner(dataSource, List.of(migration));
        runner.runMigrations();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM sales WHERE id = 41 AND user_id = 7"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM sale_items WHERE id = 51 AND sale_id = 41 "
                            + "AND product_id = 9 AND quantity = 2"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM activity_logs WHERE id = 61 AND user_id = 7 "
                            + "AND action = 'SALE'"));

            assertEquals(Set.of("users:user_id->id"),
                    foreignKeys(statement, "sales"));
            assertEquals(Set.of("sales:sale_id->id", "products:product_id->id"),
                    foreignKeys(statement, "sale_items"));
            assertEquals(Set.of("users:user_id->id"),
                    foreignKeys(statement, "activity_logs"));

            try (ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertEquals(false, resultSet.next());
            }
        }

        // A recorded migration is skipped on subsequent application startups.
        runner.runMigrations();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM sales"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM sale_items"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM activity_logs"));
        }
    }

    private void createLegacySchemaAndRows(SQLiteDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        id INTEGER PRIMARY KEY,
                        username VARCHAR(100) NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        full_name VARCHAR(150) NOT NULL,
                        role VARCHAR(10) NOT NULL,
                        active BOOLEAN NOT NULL,
                        created_at DATETIME NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE products (
                        id INTEGER PRIMARY KEY,
                        sku VARCHAR(100) NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        author VARCHAR(255),
                        price NUMERIC(10, 2) NOT NULL,
                        quantity INTEGER NOT NULL,
                        min_stock INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE sales (
                        id INTEGER PRIMARY KEY,
                        sale_date DATETIME NOT NULL,
                        customer_name VARCHAR(255),
                        total_amount NUMERIC(12, 2) NOT NULL,
                        user_id INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE sale_items (
                        id INTEGER PRIMARY KEY,
                        sale_id INTEGER NOT NULL,
                        product_id INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        unit_price NUMERIC(12, 2) NOT NULL,
                        subtotal NUMERIC(12, 2) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE activity_logs (
                        id INTEGER PRIMARY KEY,
                        user_id INTEGER NOT NULL,
                        action VARCHAR(50) NOT NULL,
                        details VARCHAR(1000),
                        created_at DATETIME NOT NULL
                    )
                    """);

            statement.execute("""
                    INSERT INTO users
                        (id, username, password_hash, full_name, role, active, created_at)
                    VALUES (7, 'legacy-admin', 'encoded', 'Legacy Admin', 'ADMIN', 1,
                            '2026-11-12T04:00:00Z')
                    """);
            statement.execute("""
                    INSERT INTO products
                        (id, sku, title, author, price, quantity, min_stock)
                    VALUES (9, 'LEGACY-9', 'Legacy Product', NULL, 12.50, 5, 1)
                    """);
            statement.execute("""
                    INSERT INTO sales
                        (id, sale_date, customer_name, total_amount, user_id)
                    VALUES (41, '2026-11-12T04:01:00Z', 'Legacy Customer', 25.00, 7)
                    """);
            statement.execute("""
                    INSERT INTO sale_items
                        (id, sale_id, product_id, quantity, unit_price, subtotal)
                    VALUES (51, 41, 9, 2, 12.50, 25.00)
                    """);
            statement.execute("""
                    INSERT INTO activity_logs
                        (id, user_id, action, details, created_at)
                    VALUES (61, 7, 'SALE', 'Legacy sale created',
                            '2026-11-12T04:01:00Z')
                    """);
        }
    }

    private int scalar(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Set<String> foreignKeys(Statement statement, String tableName)
            throws Exception {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = statement.executeQuery(
                "PRAGMA foreign_key_list('" + tableName + "')")) {
            while (rows.next()) {
                result.add(rows.getString("table") + ":"
                        + rows.getString("from") + "->"
                        + rows.getString("to"));
            }
        }
        return result;
    }
}
