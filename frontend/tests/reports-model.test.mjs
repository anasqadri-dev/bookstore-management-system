import test from "node:test";
import assert from "node:assert/strict";
import {
    buildSalesReportPath,
    filterReportRows,
    formatReportMoney,
    validateSalesDateRange,
} from "../src/lib/reports-model.mjs";

test("requires both report dates", () => {
    assert.match(validateSalesDateRange("", "2026-11-14"), /both/);
    assert.match(validateSalesDateRange("2026-11-14", ""), /both/);
});

test("rejects reversed and malformed report dates", () => {
    assert.match(validateSalesDateRange("2026-11-15", "2026-11-14"), /after/);
    assert.match(validateSalesDateRange("14-11-2026", "2026-11-15"), /YYYY-MM-DD/);
});

test("builds an encoded backend sales report path", () => {
    assert.equal(
        buildSalesReportPath("2026-11-01", "2026-11-14"),
        "/api/reports/sales?startDate=2026-11-01&endDate=2026-11-14",
    );
});

test("filters report rows by any selected field without changing the source list", () => {
    const rows = [
        { saleId: 1, customerName: "Walk-in", cashierName: "Ayesha" },
        { saleId: 2, customerName: "School", cashierName: "Bilal" },
    ];
    assert.deepEqual(filterReportRows(rows, "school", ["customerName", "cashierName"]), [rows[1]]);
    assert.equal(filterReportRows(rows, "  ", ["customerName"]).length, 2);
    assert.equal(rows.length, 2);
});

test("formats report amounts as PKR", () => {
    assert.match(formatReportMoney(1250.5), /1,250\.50/);
});
