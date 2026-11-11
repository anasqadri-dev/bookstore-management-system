package com.power_puff.bms.report.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesReportItemResponse(
        Integer saleId,
        Instant saleDate,
        String customerName,
        String cashierName,
        BigDecimal totalAmount
) {
}
