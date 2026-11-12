package com.power_puff.bms.database;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds the sales and activity-log foreign keys that the SQLite Hibernate
 * dialect cannot add with ALTER TABLE. Existing rows are copied into
 * replacement tables inside one transaction before the originals are replaced.
 */
@Component
public class SalesActivityLogForeignKeyMigration implements ApplicationRunner {

    private static final Set<String> SALES_FOREIGN_KEYS = Set.of(
            "users:user_id->id");

    private static final Set<String> SALE_ITEM_FOREIGN_KEYS = Set.of(
            "sales:sale_id->id",
            "products:product_id->id");

    private static final Set<String> ACTIVITY_LOG_FOREIGN_KEYS = Set.of(
            "users:user_id->id");

    private final DataSource dataSource;

    public SalesActivityLogForeignKeyMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");

            if (hasExpectedForeignKeys(statement)) {
                statement.execute("PRAGMA foreign_keys=ON");
                return;
            }

            connection.setAutoCommit(false);
            try {
                rebuildSalesAndActivityTables(statement);
                verifyForeignKeys(statement);
                verifyNoForeignKeyViolations(statement);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
                statement.execute("PRAGMA foreign_keys=ON");
            }

            verifyForeignKeys(statement);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not apply the sales/activity-log SQLite foreign-key migration. "
                            + "Existing sales and activity data was not intentionally discarded.",
                    exception);
        }
    }

    private boolean hasExpectedForeignKeys(Statement statement) throws SQLException {
        return SALES_FOREIGN_KEYS.equals(readForeignKeys(statement, "sales"))
                && SALE_ITEM_FOREIGN_KEYS.equals(readForeignKeys(statement, "sale_items"))
                && ACTIVITY_LOG_FOREIGN_KEYS.equals(
                        readForeignKeys(statement, "activity_logs"));
    }

    private void rebuildSalesAndActivityTables(Statement statement) throws SQLException {
        statement.execute("DROP TABLE IF EXISTS sale_items_new");
        statement.execute("DROP TABLE IF EXISTS sales_new");
        statement.execute("DROP TABLE IF EXISTS activity_logs_new");

        statement.execute("""
                CREATE TABLE sales_new (
                    id INTEGER PRIMARY KEY,
                    sale_date DATETIME NOT NULL,
                    customer_name VARCHAR(255),
                    total_amount NUMERIC(12, 2) NOT NULL,
                    user_id INTEGER NOT NULL REFERENCES users(id)
                )
                """);

        statement.execute("""
                INSERT INTO sales_new (id, sale_date, customer_name, total_amount, user_id)
                SELECT id, sale_date, customer_name, total_amount, user_id
                FROM sales
                """);

        statement.execute("""
                CREATE TABLE sale_items_new (
                    id INTEGER PRIMARY KEY,
                    sale_id INTEGER NOT NULL REFERENCES sales_new(id),
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    quantity INTEGER NOT NULL,
                    unit_price NUMERIC(12, 2) NOT NULL,
                    subtotal NUMERIC(12, 2) NOT NULL
                )
                """);

        statement.execute("""
                INSERT INTO sale_items_new
                    (id, sale_id, product_id, quantity, unit_price, subtotal)
                SELECT id, sale_id, product_id, quantity, unit_price, subtotal
                FROM sale_items
                """);

        statement.execute("""
                CREATE TABLE activity_logs_new (
                    id INTEGER PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    action VARCHAR(50) NOT NULL,
                    details VARCHAR(1000),
                    created_at DATETIME NOT NULL
                )
                """);

        statement.execute("""
                INSERT INTO activity_logs_new (id, user_id, action, details, created_at)
                SELECT id, user_id, action, details, created_at
                FROM activity_logs
                """);

        assertNoForeignKeyViolations(statement, "sales_new");
        assertNoForeignKeyViolations(statement, "sale_items_new");
        assertNoForeignKeyViolations(statement, "activity_logs_new");

        statement.execute("DROP TABLE sale_items");
        statement.execute("DROP TABLE sales");
        statement.execute("DROP TABLE activity_logs");

        statement.execute("ALTER TABLE sales_new RENAME TO sales");
        statement.execute("ALTER TABLE sale_items_new RENAME TO sale_items");
        statement.execute("ALTER TABLE activity_logs_new RENAME TO activity_logs");
    }

    private void verifyForeignKeys(Statement statement) throws SQLException {
        Set<String> salesKeys = readForeignKeys(statement, "sales");
        Set<String> saleItemKeys = readForeignKeys(statement, "sale_items");
        Set<String> activityKeys = readForeignKeys(statement, "activity_logs");

        if (!SALES_FOREIGN_KEYS.equals(salesKeys)
                || !SALE_ITEM_FOREIGN_KEYS.equals(saleItemKeys)
                || !ACTIVITY_LOG_FOREIGN_KEYS.equals(activityKeys)) {
            throw new IllegalStateException(
                    "Sales/activity-log migration did not create the expected relationships. "
                            + "sales=" + salesKeys
                            + ", sale_items=" + saleItemKeys
                            + ", activity_logs=" + activityKeys);
        }
    }

    private Set<String> readForeignKeys(Statement statement, String tableName)
            throws SQLException {
        Set<String> foreignKeys = new HashSet<>();
        try (ResultSet resultSet = statement.executeQuery(
                "PRAGMA foreign_key_list('" + tableName + "')")) {
            while (resultSet.next()) {
                foreignKeys.add(
                        resultSet.getString("table") + ":"
                                + resultSet.getString("from") + "->"
                                + resultSet.getString("to"));
            }
        }
        return foreignKeys;
    }

    private void verifyNoForeignKeyViolations(Statement statement) throws SQLException {
        assertNoForeignKeyViolations(statement, "sales");
        assertNoForeignKeyViolations(statement, "sale_items");
        assertNoForeignKeyViolations(statement, "activity_logs");
    }

    private void assertNoForeignKeyViolations(Statement statement, String tableName)
            throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "PRAGMA foreign_key_check('" + tableName + "')")) {
            if (resultSet.next()) {
                throw new IllegalStateException(
                        "Cannot enforce foreign keys because table " + tableName
                                + " contains an orphaned reference.");
            }
        }
    }
}
