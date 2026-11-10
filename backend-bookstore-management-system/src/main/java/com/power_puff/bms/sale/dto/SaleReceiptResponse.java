package com.power_puff.bms.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleReceiptResponse(
        Integer id,
        Instant saleDate,
        String customerName,
        BigDecimal totalAmount,
        String cashierName,
        List<SaleItemReceiptResponse> items
) {
}
