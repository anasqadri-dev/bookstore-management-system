package com.power_puff.bms.sale.mapper;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.sale.dto.SaleItemReceiptResponse;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.entity.SaleItem;

public final class SaleMapper {

    private SaleMapper() {
    }

    public static SaleReceiptResponse toReceipt(Sale sale) {
        return new SaleReceiptResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getCustomerName(),
                sale.getTotalAmount(),
                sale.getUser().getFullName(),
                sale.getItems().stream().map(SaleMapper::toReceiptItem).toList()
        );
    }

    private static SaleItemReceiptResponse toReceiptItem(SaleItem item) {
        Product product = item.getProduct();
        return new SaleItemReceiptResponse(
                product.getId(),
                product.getSku(),
                product.getTitle(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
