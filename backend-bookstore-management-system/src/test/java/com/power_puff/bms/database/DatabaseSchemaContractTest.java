package com.power_puff.bms.database;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseSchemaContractTest {

    private static final Map<String, Set<String>> EXPECTED_COLUMNS = Map.ofEntries(
            Map.entry("users", Set.of(
                    "id", "username", "password_hash", "full_name",
                    "role", "active", "created_at")),
            Map.entry("products", Set.of(
                    "id", "sku", "title", "author", "price", "quantity", "min_stock")),
            Map.entry("sales", Set.of(
                    "id", "sale_date", "customer_name", "total_amount", "user_id")),
            Map.entry("sale_items", Set.of(
                    "id", "sale_id", "product_id", "quantity", "unit_price", "subtotal")),
            Map.entry("schools", Set.of(
                    "id", "name", "contact_person", "phone", "address")),
            Map.entry("school_orders", Set.of(
                    "id", "school_id", "order_date", "status", "user_id")),
            Map.entry("school_order_items", Set.of(
                    "id", "order_id", "product_id", "quantity", "unit_price")),
            Map.entry("suppliers", Set.of(
                    "id", "name", "phone", "address")),
            Map.entry("purchase_orders", Set.of(
                    "id", "supplier_id", "order_date", "status", "received_date", "user_id")),
            Map.entry("purchase_order_items", Set.of(
                    "id", "purchase_order_id", "product_id", "quantity", "unit_cost")),
            Map.entry("activity_logs", Set.of(
                    "id", "user_id", "action", "details", "created_at"))
    );

    private static final Map<String, Set<String>> EXPECTED_FOREIGN_KEYS = Map.ofEntries(
            Map.entry("users", Set.of()),
            Map.entry("products", Set.of()),
            Map.entry("sales", Set.of("users:user_id->id")),
            Map.entry("sale_items", Set.of(
                    "sales:sale_id->id", "products:product_id->id")),
            Map.entry("schools", Set.of()),
            Map.entry("school_orders", Set.of(
                    "schools:school_id->id", "users:user_id->id")),
            Map.entry("school_order_items", Set.of(
                    "school_orders:order_id->id", "products:product_id->id")),
            Map.entry("suppliers", Set.of()),
            Map.entry("purchase_orders", Set.of(
                    "suppliers:supplier_id->id", "users:user_id->id")),
            Map.entry("purchase_order_items", Set.of(
                    "purchase_orders:purchase_order_id->id", "products:product_id->id")),
            Map.entry("activity_logs", Set.of("users:user_id->id"))
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaContainsExactlyTheElevenSdsTables() {
        Set<String> actualTables = new HashSet<>(jdbcTemplate.query(
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                  AND name NOT LIKE 'sqlite_%'
                  AND name <> 'schema_migrations'
                """,
                (resultSet, rowNumber) -> resultSet.getString("name")));

        assertEquals(EXPECTED_COLUMNS.keySet(), actualTables);
    }

    @Test
    void everySdsTableContainsExactlyItsDocumentedColumns() {
        for (Map.Entry<String, Set<String>> entry : EXPECTED_COLUMNS.entrySet()) {
            Set<String> actualColumns = new HashSet<>(jdbcTemplate.query(
                    "PRAGMA table_info('" + entry.getKey() + "')",
                    (resultSet, rowNumber) -> resultSet.getString("name")));

            assertEquals(entry.getValue(), actualColumns,
                    "Unexpected columns in table " + entry.getKey());
        }
    }

    @Test
    void allSdsForeignKeyRelationshipsArePresentAndNoExtraRelationshipsExist() {
        Map<String, Set<String>> actualForeignKeys = new HashMap<>();

        for (String tableName : EXPECTED_COLUMNS.keySet()) {
            Set<String> keys = new HashSet<>(jdbcTemplate.query(
                    "PRAGMA foreign_key_list('" + tableName + "')",
                    (resultSet, rowNumber) ->
                            resultSet.getString("table") + ":"
                                    + resultSet.getString("from") + "->"
                                    + resultSet.getString("to")));
            actualForeignKeys.put(tableName, keys);
        }

        assertEquals(EXPECTED_FOREIGN_KEYS, actualForeignKeys);
    }

    @Test
    void sqliteForeignKeysAreEnabledAndExistingRowsHaveNoViolations() {
        assertEquals(1, jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class));

        Integer violationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_foreign_key_check",
                Integer.class);
        assertEquals(0, violationCount);
    }

    @Test
    void noUnspecifiedTotalsOrSubtotalsAreAddedToOrderTables() {
        assertTrue(!columns("school_orders").contains("total_amount"));
        assertTrue(!columns("school_order_items").contains("subtotal"));
        assertTrue(!columns("purchase_orders").contains("total_amount"));
        assertTrue(!columns("purchase_order_items").contains("subtotal"));
    }

    private Set<String> columns(String tableName) {
        return new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info('" + tableName + "')",
                (resultSet, rowNumber) -> resultSet.getString("name")));
    }
}
