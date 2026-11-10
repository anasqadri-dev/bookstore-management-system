package com.power_puff.bms.purchase.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Integer productId,
        String sku,
        String title,
        Integer quantity,
        BigDecimal unitCost
) {
}
