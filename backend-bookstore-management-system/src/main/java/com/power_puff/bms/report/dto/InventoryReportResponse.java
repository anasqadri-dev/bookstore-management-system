package com.power_puff.bms.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryReportResponse(
        long totalProducts,
        long totalUnits,
        long lowStockProducts,
        BigDecimal totalStockValue,
        List<InventoryReportItemResponse> products
) {
}
