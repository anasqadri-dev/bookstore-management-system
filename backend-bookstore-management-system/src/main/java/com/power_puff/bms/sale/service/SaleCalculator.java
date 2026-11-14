package com.power_puff.bms.sale.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

@Component
public class SaleCalculator {

    private static final int CURRENCY_SCALE = 2;
    private static final int MAX_CURRENCY_PRECISION = 12;

    public BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity) {
        requireCurrencyAmount(unitPrice, "Unit price");
        if (unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        requireCurrencyAmount(subtotal, "Sale subtotal");

        // Money entering the sale calculation is already restricted to two
        // decimal places. UNNECESSARY makes an accidental rounding path fail
        // rather than silently changing a persisted monetary value.
        return subtotal.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY);
    }

    public BigDecimal calculateTotal(Collection<BigDecimal> subtotals) {
        Objects.requireNonNull(subtotals, "Subtotals are required.");

        BigDecimal total = subtotals.stream()
                .map(subtotal -> Objects.requireNonNull(subtotal, "Subtotal cannot be null."))
                .peek(subtotal -> {
                    requireCurrencyAmount(subtotal, "Sale subtotal");
                    if (subtotal.signum() < 0) {
                        throw new IllegalArgumentException("Subtotal cannot be negative.");
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        requireCurrencyAmount(total, "Sale total");
        return total.setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY);
    }

    private void requireCurrencyAmount(BigDecimal amount, String field) {
        if (amount == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        if (amount.scale() > CURRENCY_SCALE) {
            throw new IllegalArgumentException(
                    field + " must not contain more than two decimal places.");
        }
        if (amount.precision() > MAX_CURRENCY_PRECISION) {
            throw new IllegalArgumentException(
                    field + " exceeds the supported currency precision.");
        }
    }
}
