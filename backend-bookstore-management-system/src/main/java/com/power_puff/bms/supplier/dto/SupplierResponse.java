package com.power_puff.bms.supplier.dto;

public record SupplierResponse(
        Integer id,
        String name,
        String phone,
        String address
) {
}
