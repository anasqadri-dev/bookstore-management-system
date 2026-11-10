package com.power_puff.bms.schoolorder.config;

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
 * Adds the school-order foreign keys SQLite's Hibernate community dialect
 * cannot add with ALTER TABLE. Existing data is copied into replacement
 * tables inside one transaction before the originals are replaced.
 */
@Component
public class SchoolOrderForeignKeyMigration implements ApplicationRunner {

    private static final Set<String> SCHOOL_ORDER_FOREIGN_KEYS = Set.of(
            "schools:school_id->id",
            "users:user_id->id");

    private static final Set<String> SCHOOL_ORDER_ITEM_FOREIGN_KEYS = Set.of(
            "school_orders:order_id->id",
            "products:product_id->id");

    private final DataSource dataSource;

    public SchoolOrderForeignKeyMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");

            if (hasExpectedForeignKeys(statement)) {
                statement.execute("PRAGMA foreign_keys=ON");
                return;
            }

            connection.setAutoCommit(false);
            try {
                rebuildSchoolOrderTables(statement);
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
                    "Could not apply the school-order SQLite foreign-key migration. "
                            + "The existing school-order tables were not intentionally discarded.",
                    exception);
        }
    }

    private boolean hasExpectedForeignKeys(Statement statement) throws SQLException {
        return SCHOOL_ORDER_FOREIGN_KEYS.equals(readForeignKeys(statement, "school_orders"))
                && SCHOOL_ORDER_ITEM_FOREIGN_KEYS.equals(
                        readForeignKeys(statement, "school_order_items"));
    }

    private void rebuildSchoolOrderTables(Statement statement) throws SQLException {
        statement.execute("DROP TABLE IF EXISTS school_order_items_new");
        statement.execute("DROP TABLE IF EXISTS school_orders_new");

        statement.execute("""
                CREATE TABLE school_orders_new (
                    id INTEGER PRIMARY KEY,
                    school_id INTEGER NOT NULL REFERENCES schools(id),
                    order_date DATE NOT NULL,
                    status VARCHAR(20) NOT NULL
                        CHECK (status IN ('PENDING', 'READY', 'DELIVERED')),
                    user_id INTEGER NOT NULL REFERENCES users(id)
                )
                """);

        statement.execute("""
                INSERT INTO school_orders_new (id, school_id, order_date, status, user_id)
                SELECT id, school_id, order_date, status, user_id
                FROM school_orders
                """);

        statement.execute("""
                CREATE TABLE school_order_items_new (
                    id INTEGER PRIMARY KEY,
                    order_id INTEGER NOT NULL REFERENCES school_orders_new(id),
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    quantity INTEGER NOT NULL CHECK (quantity >= 1),
                    unit_price NUMERIC(12, 2) NOT NULL
                )
                """);

        statement.execute("""
                INSERT INTO school_order_items_new
                    (id, order_id, product_id, quantity, unit_price)
                SELECT id, order_id, product_id, quantity, unit_price
                FROM school_order_items
                """);

        assertNoForeignKeyViolations(statement, "school_orders_new");
        assertNoForeignKeyViolations(statement, "school_order_items_new");

        statement.execute("DROP TABLE school_order_items");
        statement.execute("DROP TABLE school_orders");
        statement.execute("ALTER TABLE school_orders_new RENAME TO school_orders");
        statement.execute("ALTER TABLE school_order_items_new RENAME TO school_order_items");
    }

    private void verifyForeignKeys(Statement statement) throws SQLException {
        Set<String> orderKeys = readForeignKeys(statement, "school_orders");
        Set<String> itemKeys = readForeignKeys(statement, "school_order_items");

        if (!SCHOOL_ORDER_FOREIGN_KEYS.equals(orderKeys)
                || !SCHOOL_ORDER_ITEM_FOREIGN_KEYS.equals(itemKeys)) {
            throw new IllegalStateException(
                    "School-order foreign-key migration did not create the expected relationships. "
                            + "school_orders=" + orderKeys
                            + ", school_order_items=" + itemKeys);
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
        assertNoForeignKeyViolations(statement, "school_orders");
        assertNoForeignKeyViolations(statement, "school_order_items");
    }

    private void assertNoForeignKeyViolations(Statement statement, String tableName)
            throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "PRAGMA foreign_key_check('" + tableName + "')")) {
            if (resultSet.next()) {
                throw new IllegalStateException(
                        "Cannot enforce school-order foreign keys because table "
                                + tableName + " contains an orphaned reference.");
            }
        }
    }
}
