package com.power_puff.bms.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String title,
        String author,
        BigDecimal price,
        Integer quantity,
        Integer minStock,
        boolean lowStock
) {
}
