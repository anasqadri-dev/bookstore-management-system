package com.power_puff.bms.purchase.config;

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
 * Adds the purchase-order foreign keys SQLite's Hibernate community dialect
 * cannot add with ALTER TABLE. Existing rows are copied into replacement
 * tables inside one transaction before the originals are replaced.
 */
@Component
public class PurchaseOrderForeignKeyMigration implements ApplicationRunner {

    private static final Set<String> PURCHASE_ORDER_FOREIGN_KEYS = Set.of(
            "suppliers:supplier_id->id",
            "users:user_id->id");

    private static final Set<String> PURCHASE_ORDER_ITEM_FOREIGN_KEYS = Set.of(
            "purchase_orders:purchase_order_id->id",
            "products:product_id->id");

    private final DataSource dataSource;

    public PurchaseOrderForeignKeyMigration(DataSource dataSource) {
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
                rebuildPurchaseOrderTables(statement);
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
                    "Could not apply the purchase-order SQLite foreign-key migration. "
                            + "Existing purchase-order data was not intentionally discarded.",
                    exception);
        }
    }

    private boolean hasExpectedForeignKeys(Statement statement) throws SQLException {
        return PURCHASE_ORDER_FOREIGN_KEYS.equals(
                readForeignKeys(statement, "purchase_orders"))
                && PURCHASE_ORDER_ITEM_FOREIGN_KEYS.equals(
                        readForeignKeys(statement, "purchase_order_items"));
    }

    private void rebuildPurchaseOrderTables(Statement statement) throws SQLException {
        statement.execute("DROP TABLE IF EXISTS purchase_order_items_new");
        statement.execute("DROP TABLE IF EXISTS purchase_orders_new");

        statement.execute("""
                CREATE TABLE purchase_orders_new (
                    id INTEGER PRIMARY KEY,
                    supplier_id INTEGER NOT NULL REFERENCES suppliers(id),
                    order_date DATE NOT NULL,
                    status VARCHAR(20) NOT NULL
                        CHECK (status IN ('ORDERED', 'RECEIVED')),
                    received_date DATE,
                    user_id INTEGER NOT NULL REFERENCES users(id)
                )
                """);

        statement.execute("""
                INSERT INTO purchase_orders_new
                    (id, supplier_id, order_date, status, received_date, user_id)
                SELECT id, supplier_id, order_date, status, received_date, user_id
                FROM purchase_orders
                """);

        statement.execute("""
                CREATE TABLE purchase_order_items_new (
                    id INTEGER PRIMARY KEY,
                    purchase_order_id INTEGER NOT NULL
                        REFERENCES purchase_orders_new(id),
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    quantity INTEGER NOT NULL CHECK (quantity >= 1),
                    unit_cost NUMERIC(12, 2) NOT NULL CHECK (unit_cost > 0)
                )
                """);

        statement.execute("""
                INSERT INTO purchase_order_items_new
                    (id, purchase_order_id, product_id, quantity, unit_cost)
                SELECT id, purchase_order_id, product_id, quantity, unit_cost
                FROM purchase_order_items
                """);

        assertNoForeignKeyViolations(statement, "purchase_orders_new");
        assertNoForeignKeyViolations(statement, "purchase_order_items_new");

        statement.execute("DROP TABLE purchase_order_items");
        statement.execute("DROP TABLE purchase_orders");
        statement.execute("ALTER TABLE purchase_orders_new RENAME TO purchase_orders");
        statement.execute(
                "ALTER TABLE purchase_order_items_new RENAME TO purchase_order_items");
    }

    private void verifyForeignKeys(Statement statement) throws SQLException {
        Set<String> orderKeys = readForeignKeys(statement, "purchase_orders");
        Set<String> itemKeys = readForeignKeys(statement, "purchase_order_items");

        if (!PURCHASE_ORDER_FOREIGN_KEYS.equals(orderKeys)
                || !PURCHASE_ORDER_ITEM_FOREIGN_KEYS.equals(itemKeys)) {
            throw new IllegalStateException(
                    "Purchase-order foreign-key migration did not create the expected relationships. "
                            + "purchase_orders=" + orderKeys
                            + ", purchase_order_items=" + itemKeys);
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
        assertNoForeignKeyViolations(statement, "purchase_orders_new");
        assertNoForeignKeyViolations(statement, "purchase_order_items_new");
    }

    private void assertNoForeignKeyViolations(Statement statement, String tableName)
            throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "PRAGMA foreign_key_check('" + tableName + "')")) {
            if (resultSet.next()) {
                throw new IllegalStateException(
                        "Cannot enforce purchase-order foreign keys because table "
                                + tableName + " contains an orphaned reference.");
            }
        }
    }
}
