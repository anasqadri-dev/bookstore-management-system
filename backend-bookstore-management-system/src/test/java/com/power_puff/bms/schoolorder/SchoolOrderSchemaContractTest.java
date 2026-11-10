package com.power_puff.bms.schoolorder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SchoolOrderSchemaContractTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schoolTableContainsOnlySdsDataDictionaryColumns() {
        assertEquals(
                Set.of("id", "name", "contact_person", "phone", "address"),
                columns("schools"));
    }

    @Test
    void schoolOrderTablesContainOnlySdsDataDictionaryColumns() {
        assertEquals(
                Set.of("id", "school_id", "order_date", "status", "user_id"),
                columns("school_orders"));

        assertEquals(
                Set.of("id", "order_id", "product_id", "quantity", "unit_price"),
                columns("school_order_items"));
    }

    @Test
    void schoolOrdersHaveTheSdsForeignKeyRelationships() {
        assertEquals(
                Set.of("schools:school_id->id", "users:user_id->id"),
                foreignKeys("school_orders"));

        assertEquals(
                Set.of("school_orders:order_id->id", "products:product_id->id"),
                foreignKeys("school_order_items"));
    }

    @Test
    void schoolOrderSchemaDoesNotAddUnspecifiedTotalsOrSubtotals() {
        Set<String> orderColumns = columns("school_orders");
        Set<String> itemColumns = columns("school_order_items");

        assertTrue(!orderColumns.contains("total_amount"));
        assertTrue(!itemColumns.contains("subtotal"));
    }

    private Set<String> columns(String tableName) {
        return new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info('" + tableName + "')",
                (resultSet, rowNumber) -> resultSet.getString("name")));
    }

    private Set<String> foreignKeys(String tableName) {
        return new HashSet<>(jdbcTemplate.query(
                "PRAGMA foreign_key_list('" + tableName + "')",
                (resultSet, rowNumber) ->
                        resultSet.getString("table") + ":"
                                + resultSet.getString("from") + "->"
                                + resultSet.getString("to")));
    }
}
