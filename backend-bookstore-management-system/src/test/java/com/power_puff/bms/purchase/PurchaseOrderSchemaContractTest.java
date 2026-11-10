package com.power_puff.bms.purchase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseOrderSchemaContractTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void supplierTableContainsOnlySdsDataDictionaryColumns() {
        assertEquals(
                Set.of("id", "name", "phone", "address"),
                columns("suppliers"));
    }

    @Test
    void purchaseOrderTablesContainOnlySdsDataDictionaryColumns() {
        assertEquals(
                Set.of("id", "supplier_id", "order_date", "status", "received_date", "user_id"),
                columns("purchase_orders"));

        assertEquals(
                Set.of("id", "purchase_order_id", "product_id", "quantity", "unit_cost"),
                columns("purchase_order_items"));
    }

    @Test
    void purchaseOrderTablesHaveTheSdsForeignKeyRelationships() {
        assertEquals(
                Set.of("suppliers:supplier_id->id", "users:user_id->id"),
                foreignKeys("purchase_orders"));

        assertEquals(
                Set.of("purchase_orders:purchase_order_id->id", "products:product_id->id"),
                foreignKeys("purchase_order_items"));
    }

    @Test
    void schemaDoesNotAddUnspecifiedPurchaseTotalsOrItemSubtotals() {
        assertTrue(!columns("purchase_orders").contains("total_amount"));
        assertTrue(!columns("purchase_order_items").contains("subtotal"));
    }

    @Test
    void sqliteEnforcesPurchaseOrderForeignKeys() {
        assertEquals(1, jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class));

        DataAccessException orderFailure = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO purchase_orders
                            (supplier_id, order_date, status, received_date, user_id)
                        VALUES (?, ?, ?, ?, ?)
                        """, 999999, "2026-11-11", "ORDERED", null, 999999));
        assertTrue(orderFailure.getMessage().contains("FOREIGN KEY constraint failed"));

        DataAccessException itemFailure = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO purchase_order_items
                            (purchase_order_id, product_id, quantity, unit_cost)
                        VALUES (?, ?, ?, ?)
                        """, 999999, 999999, 1, "10.00"));
        assertTrue(itemFailure.getMessage().contains("FOREIGN KEY constraint failed"));
    }

    @Test
    void sqliteRejectsUnsupportedPurchaseOrderStatus() {
        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO purchase_orders
                            (supplier_id, order_date, status, received_date, user_id)
                        VALUES (?, ?, ?, ?, ?)
                        """, 999999, "2026-11-11", "COMPLETED", null, 999999));

        assertTrue(exception.getMessage().contains("CHECK constraint failed"));
    }

    private Set<String> columns(String tableName) {
        return new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info('" + tableName + "')",
                (resultSet, rowNumber) -> resultSet.getString("name")));
    }

    private Set<String> foreignKeys(String tableName) {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT \"table\", \"from\", \"to\" " +
                        "FROM pragma_foreign_key_list('" + tableName + "')",
                (resultSet, rowNumber) ->
                        resultSet.getString("table") + ":"
                                + resultSet.getString("from") + "->"
                                + resultSet.getString("to")));
    }
}
