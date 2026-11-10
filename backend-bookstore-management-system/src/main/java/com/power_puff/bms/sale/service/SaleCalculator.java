package com.power_puff.bms.sale.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

@Component
public class SaleCalculator {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);
    }

    public BigDecimal calculateTotal(Collection<BigDecimal> subtotals) {
        Objects.requireNonNull(subtotals, "Subtotals are required.");

        BigDecimal total = subtotals.stream()
                .map(subtotal -> Objects.requireNonNull(subtotal, "Subtotal cannot be null."))
                .peek(subtotal -> {
                    if (subtotal.signum() < 0) {
                        throw new IllegalArgumentException("Subtotal cannot be negative.");
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(CURRENCY_SCALE, ROUNDING_MODE);
    }
}
