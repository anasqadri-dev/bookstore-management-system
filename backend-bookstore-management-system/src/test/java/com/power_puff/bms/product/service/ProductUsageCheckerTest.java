package com.power_puff.bms.product.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductUsageCheckerTest {

    @Test
    void returnsFalseWhenReferenceTablesDoNotExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0);

        ProductUsageChecker checker = new ProductUsageChecker(jdbcTemplate);

        assertFalse(checker.isReferenced(10));
        verify(jdbcTemplate, times(3)).queryForObject(anyString(), eq(Integer.class), any());
    }

    @Test
    void returnsTrueWhenProductIsReferencedBySaleItem() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(1, 1);

        ProductUsageChecker checker = new ProductUsageChecker(jdbcTemplate);

        assertTrue(checker.isReferenced(10));
    }
}
