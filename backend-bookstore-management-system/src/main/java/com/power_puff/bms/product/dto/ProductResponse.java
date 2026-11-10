package com.power_puff.bms.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        String sku,
        String title,
        String author,
        BigDecimal price,
        Integer quantity,
        Integer minStock,
        boolean lowStock
) {
}
