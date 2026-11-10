package com.power_puff.bms.product.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Checks the SDS-defined product reference tables without assuming that
 * downstream modules have already created their tables.
 */
@Component
public class ProductUsageChecker {

    private static final List<String> REFERENCE_TABLES = List.of(
            "sale_items",
            "school_order_items",
            "purchase_order_items"
    );

    private final JdbcTemplate jdbcTemplate;

    public ProductUsageChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isReferenced(Long productId) {
        for (String table : REFERENCE_TABLES) {
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                    Integer.class,
                    table
            );
            if (tableExists == null || tableExists == 0) {
                continue;
            }

            Integer referenceCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE product_id = ?",
                    Integer.class,
                    productId
            );
            if (referenceCount != null && referenceCount > 0) {
                return true;
            }
        }
        return false;
    }
}
