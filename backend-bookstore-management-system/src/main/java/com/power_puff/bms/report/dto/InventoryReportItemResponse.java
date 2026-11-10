package com.power_puff.bms.report.dto;

import java.math.BigDecimal;

public record InventoryReportItemResponse(
        Integer productId,
        String sku,
        String title,
        String author,
        BigDecimal price,
        Integer quantity,
        Integer minStock,
        boolean lowStock,
        BigDecimal stockValue
) {
}
