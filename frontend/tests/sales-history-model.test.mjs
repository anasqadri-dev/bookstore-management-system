import test from "node:test";
import assert from "node:assert/strict";
import { filterSavedSales, summarizeSavedSales } from "../src/lib/sales-history-model.mjs";

const sales = [
    {
        id: 101,
        saleDate: "2026-11-14T04:00:00Z",
        customerName: "Walk-in",
        cashierName: "Anas",
        totalAmount: 525.5,
        items: [
            { sku: "BK-01", title: "Clean Code", quantity: 2, unitPrice: 200, subtotal: 400 },
            { sku: "BK-02", title: "Refactoring", quantity: 1, unitPrice: 125.5, subtotal: 125.5 },
        ],
    },
    {
        id: 102,
        saleDate: "2026-11-13T18:00:00Z",
        customerName: null,
        cashierName: "Staff",
        totalAmount: 100,
        items: [{ sku: "BK-03", title: "Algorithms", quantity: 1, unitPrice: 100, subtotal: 100 }],
    },
];

test("sales search includes saved transaction, customer, cashier, SKU and multi-item titles", () => {
    assert.deepEqual(filterSavedSales(sales, "refactoring", ""), [sales[0]]);
    assert.deepEqual(filterSavedSales(sales, "BK-03", ""), [sales[1]]);
    assert.deepEqual(filterSavedSales(sales, "102", ""), [sales[1]]);
    assert.deepEqual(filterSavedSales(sales, "staff", ""), [sales[1]]);
});

test("date filtering uses Pakistan business dates", () => {
    assert.deepEqual(filterSavedSales(sales, "", "2026-11-14"), [sales[0]]);
    assert.deepEqual(filterSavedSales(sales, "", "2026-11-13"), [sales[1]]);
});

test("sales summary counts transactions, every item quantity, and saved totals", () => {
    assert.deepEqual(summarizeSavedSales(sales), {
        transactionCount: 2,
        revenueCents: 62550,
        bookQuantity: 4,
    });
});

test("empty sales produce a genuine zero summary", () => {
    assert.deepEqual(summarizeSavedSales([]), {
        transactionCount: 0,
        revenueCents: 0,
        bookQuantity: 0,
    });
});
