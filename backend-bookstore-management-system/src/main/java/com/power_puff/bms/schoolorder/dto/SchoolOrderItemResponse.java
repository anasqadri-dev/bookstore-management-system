package com.power_puff.bms.schoolorder.dto;

import java.math.BigDecimal;

public record SchoolOrderItemResponse(
        Integer productId,
        String sku,
        String title,
        Integer quantity,
        BigDecimal unitPrice
) {
}
