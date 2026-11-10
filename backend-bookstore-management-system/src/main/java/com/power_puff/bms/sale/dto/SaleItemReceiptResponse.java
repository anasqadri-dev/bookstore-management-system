package com.power_puff.bms.sale.dto;

import java.math.BigDecimal;

public record SaleItemReceiptResponse(
        Integer productId,
        String sku,
        String title,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
