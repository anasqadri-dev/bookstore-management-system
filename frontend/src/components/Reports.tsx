"use client";

import { useMemo, useState, type FormEvent } from "react";
import { apiRequest, ApiError } from "../lib/api";
import {
    buildSalesReportPath,
    filterReportRows,
    formatReportMoney,
    validateSalesDateRange,
} from "../lib/reports-model.mjs";

type ReportType = "sales" | "inventory";
type SalesReport = {
    startDate: string;
    endDate: string;
    totalSales: number;
    totalAmount: number;
    sales: Array<{
        saleId: number;
        saleDate: string;
        customerName: string | null;
        cashierName: string;
        totalAmount: number;
    }>;
};
type InventoryReport = {
    totalProducts: number;
    totalUnits: number;
    lowStockProducts: number;
    totalStockValue: number;
    products: Array<{
        productId: number;
        sku: string;
        title: string;
        author: string | null;
        price: number;
        quantity: number;
        minStock: number;
        lowStock: boolean;
        stockValue: number;
    }>;
};

export default function Reports() {
    const [reportType, setReportType] = useState<ReportType>("sales");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [search, setSearch] = useState("");
    const [salesReport, setSalesReport] = useState<SalesReport | null>(null);
    const [inventoryReport, setInventoryReport] = useState<InventoryReport | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [validationError, setValidationError] = useState("");

    async function generateReport(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError("");
        setValidationError("");
        setSearch("");

        if (reportType === "sales") {
            const validation = validateSalesDateRange(startDate, endDate);
            if (validation) {
                setValidationError(validation);
                setSalesReport(null);
                return;
            }
        }

        setLoading(true);
        setSalesReport(null);
        setInventoryReport(null);
        try {
            if (reportType === "sales") {
                const result = await apiRequest<SalesReport>(
                    buildSalesReportPath(startDate, endDate),
                    { cache: "no-store" },
                );
                if (!result || !Array.isArray(result.sales)
                    || !Number.isFinite(result.totalSales)
                    || !Number.isFinite(result.totalAmount)) {
                    throw new Error("The server returned an invalid sales report.");
                }
                setSalesReport(result);
            } else {
                const result = await apiRequest<InventoryReport>(
                    "/api/reports/inventory",
                    { cache: "no-store" },
                );
                if (!result || !Array.isArray(result.products)
                    || !Number.isFinite(result.totalProducts)
                    || !Number.isFinite(result.totalUnits)
                    || !Number.isFinite(result.lowStockProducts)
                    || !Number.isFinite(result.totalStockValue)) {
                    throw new Error("The server returned an invalid inventory report.");
                }
                setInventoryReport(result);
            }
        } catch (cause) {
            if (cause instanceof ApiError && cause.status === 403) {
                setError("Reports are available to Admin accounts only.");
            } else if (cause instanceof ApiError && cause.status === 401) {
                setError("Your session has expired. Sign in again to view reports.");
            } else {
                setError(cause instanceof Error ? cause.message : "Unable to generate the report.");
            }
        } finally {
            setLoading(false);
        }
    }

    const salesRows = useMemo(() => filterReportRows(
        salesReport?.sales ?? [],
        search,
        ["saleId", "customerName", "cashierName"],
    ), [salesReport, search]);

    const inventoryRows = useMemo(() => filterReportRows(
        inventoryReport?.products ?? [],
        search,
        ["sku", "title", "author"],
    ), [inventoryReport, search]);

    const hasReport = reportType === "sales" ? salesReport !== null : inventoryReport !== null;

    return (
        <div className="space-y-6">
            <section className="print:hidden rounded border border-gray-200 bg-white p-6">
                <div className="mb-5">
                    <h3 className="text-xl font-semibold">Generate Reports</h3>
                    <p className="mt-1 text-sm text-gray-500">
                        Reports are calculated by the local backend from saved bookstore records.
                    </p>
                </div>
                <form onSubmit={(event) => void generateReport(event)} className="space-y-4" aria-busy={loading}>
                    <div className="flex flex-wrap gap-2" role="group" aria-label="Report type">
                        <button type="button" aria-pressed={reportType === "sales"}
                            onClick={() => { setReportType("sales"); setSalesReport(null); setInventoryReport(null); setError(""); setValidationError(""); }}
                            className={reportType === "sales" ? "rounded bg-slate-900 px-4 py-2 text-white" : "rounded border px-4 py-2"}>
                            Sales report
                        </button>
                        <button type="button" aria-pressed={reportType === "inventory"}
                            onClick={() => { setReportType("inventory"); setSalesReport(null); setInventoryReport(null); setError(""); setValidationError(""); }}
                            className={reportType === "inventory" ? "rounded bg-slate-900 px-4 py-2 text-white" : "rounded border px-4 py-2"}>
                            Inventory report
                        </button>
                    </div>

                    {reportType === "sales" && (
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label htmlFor="reports-start-date" className="mb-2 block text-sm font-medium">Start date</label>
                                <input id="reports-start-date" type="date" required value={startDate}
                                    onChange={(event) => setStartDate(event.target.value)}
                                    className="w-full rounded border border-gray-300 p-3" />
                            </div>
                            <div>
                                <label htmlFor="reports-end-date" className="mb-2 block text-sm font-medium">End date</label>
                                <input id="reports-end-date" type="date" required value={endDate}
                                    onChange={(event) => setEndDate(event.target.value)}
                                    min={startDate || undefined}
                                    className="w-full rounded border border-gray-300 p-3" />
                            </div>
                        </div>
                    )}

                    {validationError && <p role="alert" className="text-sm text-red-700">{validationError}</p>}
                    {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
                    <div className="flex flex-wrap gap-3">
                        <button type="submit" disabled={loading}
                            className="rounded bg-blue-600 px-5 py-3 text-white disabled:opacity-50">
                            {loading ? "Generating..." : "Generate report"}
                        </button>
                        {hasReport && (
                            <button type="button" onClick={() => window.print()}
                                className="rounded border border-gray-300 px-5 py-3 hover:bg-gray-50">
                                Print report
                            </button>
                        )}
                    </div>
                </form>
            </section>

            {loading && <p role="status" className="rounded border bg-white p-6">Loading report...</p>}

            {salesReport && reportType === "sales" && (
                <section className="space-y-5" aria-label="Sales report results">
                    <header className="hidden print:block">
                        <h1 className="text-2xl font-bold">Bookstore Sales Report</h1>
                        <p>{salesReport.startDate} to {salesReport.endDate}</p>
                    </header>
                    <div className="grid gap-4 sm:grid-cols-2">
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Total sales</p><p className="mt-2 text-2xl font-bold">{salesReport.totalSales.toLocaleString("en-PK")}</p></article>
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Sales amount</p><p className="mt-2 text-2xl font-bold">{formatReportMoney(salesReport.totalAmount)}</p></article>
                    </div>
                    <section className="rounded border bg-white p-5">
                        <div className="print:hidden mb-4">
                            <label htmlFor="reports-search" className="sr-only">Search report rows</label>
                            <input id="reports-search" type="search" value={search} onChange={(event) => setSearch(event.target.value)}
                                placeholder="Search sale ID, customer or cashier..."
                                className="w-full rounded border border-gray-300 px-3 py-2" />
                        </div>
                        <h2 className="mb-4 text-lg font-semibold">Sales details</h2>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-gray-100"><tr><th className="p-3">Sale ID</th><th className="p-3">Date</th><th className="p-3">Customer</th><th className="p-3">Cashier</th><th className="p-3">Amount</th></tr></thead>
                                <tbody>{salesRows.length === 0 ? <tr><td colSpan={5} className="p-8 text-center text-gray-500">{salesReport.sales.length === 0 ? "No sales were recorded in this date range." : "No sales match your search."}</td></tr> : salesRows.map((sale) => (
                                    <tr key={sale.saleId} className="border-t"><td className="p-3">#{sale.saleId}</td><td className="p-3 whitespace-nowrap">{new Date(sale.saleDate).toLocaleString("en-PK")}</td><td className="p-3">{sale.customerName || "Walk-in Customer"}</td><td className="p-3">{sale.cashierName}</td><td className="p-3 whitespace-nowrap">{formatReportMoney(sale.totalAmount)}</td></tr>
                                ))}</tbody>
                            </table>
                        </div>
                    </section>
                </section>
            )}

            {inventoryReport && reportType === "inventory" && (
                <section className="space-y-5" aria-label="Inventory report results">
                    <header className="hidden print:block">
                        <h1 className="text-2xl font-bold">Bookstore Inventory Report</h1>
                        <p>Generated from the current saved inventory.</p>
                    </header>
                    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Products</p><p className="mt-2 text-2xl font-bold">{inventoryReport.totalProducts.toLocaleString("en-PK")}</p></article>
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Total units</p><p className="mt-2 text-2xl font-bold">{inventoryReport.totalUnits.toLocaleString("en-PK")}</p></article>
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Low stock</p><p className="mt-2 text-2xl font-bold">{inventoryReport.lowStockProducts.toLocaleString("en-PK")}</p></article>
                        <article className="rounded border bg-white p-5"><p className="text-sm text-gray-500">Stock value</p><p className="mt-2 text-2xl font-bold">{formatReportMoney(inventoryReport.totalStockValue)}</p></article>
                    </div>
                    <section className="rounded border bg-white p-5">
                        <div className="print:hidden mb-4">
                            <label htmlFor="reports-search-inventory" className="sr-only">Search inventory report</label>
                            <input id="reports-search-inventory" type="search" value={search} onChange={(event) => setSearch(event.target.value)}
                                placeholder="Search SKU, title or author..."
                                className="w-full rounded border border-gray-300 px-3 py-2" />
                        </div>
                        <h2 className="mb-4 text-lg font-semibold">Inventory details</h2>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-gray-100"><tr><th className="p-3">SKU</th><th className="p-3">Book</th><th className="p-3">Author</th><th className="p-3">Price</th><th className="p-3">Quantity</th><th className="p-3">Min. stock</th><th className="p-3">Stock value</th><th className="p-3">Status</th></tr></thead>
                                <tbody>{inventoryRows.length === 0 ? <tr><td colSpan={8} className="p-8 text-center text-gray-500">{inventoryReport.products.length === 0 ? "No products are registered." : "No products match your search."}</td></tr> : inventoryRows.map((product) => (
                                    <tr key={product.productId} className="border-t"><td className="p-3 font-mono text-xs">{product.sku}</td><td className="p-3">{product.title}</td><td className="p-3">{product.author || "—"}</td><td className="p-3 whitespace-nowrap">{formatReportMoney(product.price)}</td><td className="p-3">{product.quantity}</td><td className="p-3">{product.minStock}</td><td className="p-3 whitespace-nowrap">{formatReportMoney(product.stockValue)}</td><td className="p-3">{product.lowStock ? "Low stock" : "In stock"}</td></tr>
                                ))}</tbody>
                            </table>
                        </div>
                    </section>
                </section>
            )}
        </div>
    );
}
