package com.power_puff.bms.sale.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaleCalculatorTest {

    private final SaleCalculator calculator = new SaleCalculator();

    @Test
    void calculatesLineSubtotalUsingDecimalArithmetic() {
        assertEquals(0, new BigDecimal("0.30").compareTo(
                calculator.calculateSubtotal(new BigDecimal("0.10"), 3)));
    }

    @Test
    void calculatesTotalFromAllLineSubtotals() {
        BigDecimal total = calculator.calculateTotal(List.of(
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("0.30")));

        assertEquals(0, new BigDecimal("250.30").compareTo(total));
        assertEquals(2, total.scale());
    }

    @Test
    void returnsTwoDecimalZeroForAnEmptySubtotalCollection() {
        BigDecimal total = calculator.calculateTotal(List.of());

        assertEquals(0, new BigDecimal("0.00").compareTo(total));
        assertEquals(2, total.scale());
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateSubtotal(new BigDecimal("10.00"), 0));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateSubtotal(new BigDecimal("10.00"), -1));
    }

    @Test
    void rejectsNullOrNonPositiveUnitPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateSubtotal(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateSubtotal(BigDecimal.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateSubtotal(new BigDecimal("-1.00"), 1));
    }

    @Test
    void rejectsNegativeSubtotals() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateTotal(List.of(new BigDecimal("-1.00"))));
    }
}
