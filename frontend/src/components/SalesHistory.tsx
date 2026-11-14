"use client";

import { useEffect, useState } from "react";
import { filterSavedSales, summarizeSavedSales } from "../lib/sales-history-model.mjs";
import { apiRequest } from "../lib/api";

type SaleItem = {
    productId: number;
    sku: string;
    title: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
};

type Sale = {
    id: number;
    saleDate: string;
    customerName: string | null;
    totalAmount: number;
    cashierName: string;
    items: SaleItem[];
};

const money = (amount: number) =>
    amount.toLocaleString("en-PK", {
        style: "currency",
        currency: "PKR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });

function displayDate(value: string) {
    return new Intl.DateTimeFormat("en-PK", {
        timeZone: "Asia/Karachi",
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value));
}

export default function SalesHistory() {
    const [sales, setSales] = useState<Sale[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [search, setSearch] = useState("");
    const [date, setDate] = useState("");
    const [selected, setSelected] = useState<Sale | null>(null);
    const [paperWidth, setPaperWidth] = useState<"58" | "80" | "standard">("80");

    useEffect(() => {
        let active = true;
        apiRequest<Sale[]>("/api/sales", { cache: "no-store" })
            .then((data) => {
                if (!Array.isArray(data)) {
                    throw new Error("The sales history response was invalid.");
                }
                if (active) setSales(data);
            })
            .catch((error: unknown) => {
                if (active) {
                    setLoadError(error instanceof Error
                        ? error.message
                        : "Unable to load saved sales.");
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => { active = false; };
    }, []);

    const filtered = filterSavedSales(sales, search, date);
    const summary = summarizeSavedSales(filtered);

    return (
        <div className="space-y-6">
            <div>
                <h3 className="text-xl font-semibold">Sales History</h3>
                <p className="mt-1 text-sm text-gray-500">
                    Saved cash transactions retrieved from the bookstore database.
                </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
                <div className="rounded-lg border border-gray-200 bg-white p-6">
                    <p className="text-sm text-gray-500">Transactions</p>
                    <p className="mt-3 text-2xl font-bold">{summary.transactionCount}</p>
                </div>
                <div className="rounded-lg border border-gray-200 bg-white p-6">
                    <p className="text-sm text-gray-500">Revenue</p>
                    <p className="mt-3 text-2xl font-bold">{money(summary.revenueCents / 100)}</p>
                </div>
                <div className="rounded-lg border border-gray-200 bg-white p-6">
                    <p className="text-sm text-gray-500">Books Sold</p>
                    <p className="mt-3 text-2xl font-bold">{summary.bookQuantity}</p>
                </div>
            </div>

            <div className="rounded-lg border border-gray-200 bg-white p-6">
                <div className="mb-5 flex flex-wrap gap-3">
                    <label htmlFor="sales-history-search" className="sr-only">
                        Search sales by ID, customer, cashier, SKU or book
                    </label>
                    <input
                        id="sales-history-search"
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search saved sales..."
                        className="min-w-0 flex-1 rounded-md border border-gray-300 px-4 py-3 text-sm outline-none focus:border-slate-600"
                    />
                    <label htmlFor="sales-history-date" className="sr-only">Filter sales by date</label>
                    <input
                        id="sales-history-date"
                        type="date"
                        value={date}
                        onChange={(event) => setDate(event.target.value)}
                        className="rounded-md border border-gray-300 px-4 py-3 text-sm"
                    />
                    <button
                        type="button"
                        onClick={() => { setSearch(""); setDate(""); }}
                        className="rounded-md border px-4 py-3 text-sm hover:bg-gray-50"
                    >
                        Clear
                    </button>
                </div>

                {loading ? (
                    <p role="status" className="p-10 text-center text-sm text-gray-500">
                        Loading saved sales...
                    </p>
                ) : loadError ? (
                    <div role="alert" className="rounded-md bg-red-50 p-4 text-sm text-red-800">
                        {loadError}
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-100 text-gray-600">
                                <tr>
                                    {["Sale ID", "Date", "Customer", "Items", "Qty", "Total", "Cashier", "Action"].map((heading) => (
                                        <th key={heading} scope="col" className="whitespace-nowrap p-4">{heading}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.length === 0 ? (
                                    <tr>
                                        <td colSpan={8} className="p-12 text-center text-gray-500">
                                            {sales.length === 0 ? "No saved sales are available." : "No sales match these filters."}
                                        </td>
                                    </tr>
                                ) : filtered.map((sale) => (
                                    <tr key={sale.id} className="border-b">
                                        <td className="p-4 font-medium">#{sale.id}</td>
                                        <td className="whitespace-nowrap p-4">{displayDate(sale.saleDate)}</td>
                                        <td className="p-4">{sale.customerName || "Walk-in Customer"}</td>
                                        <td className="max-w-xs p-4">{sale.items.map((item) => item.title).join(", ")}</td>
                                        <td className="p-4">{sale.items.reduce((sum, item) => sum + item.quantity, 0)}</td>
                                        <td className="whitespace-nowrap p-4">{money(sale.totalAmount)}</td>
                                        <td className="p-4">{sale.cashierName}</td>
                                        <td className="p-4">
                                            <button
                                                type="button"
                                                aria-label={`View saved receipt for sale ${sale.id}`}
                                                onClick={() => setSelected(sale)}
                                                className="font-medium text-blue-700 hover:underline"
                                            >
                                                View receipt
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {selected && (
                <section aria-labelledby="sale-history-detail-title" className="rounded-lg border bg-white p-6">
                    <div className="print:hidden mb-4 flex flex-wrap items-center justify-between gap-3">
                        <h3 id="sale-history-detail-title" className="text-lg font-semibold">
                            Saved Receipt — #{selected.id}
                        </h3>
                        <div className="flex flex-wrap items-center gap-2">
                            <label htmlFor="history-receipt-paper" className="text-sm">Paper</label>
                            <select id="history-receipt-paper" value={paperWidth}
                                onChange={(event) => setPaperWidth(event.target.value as "58" | "80" | "standard")}
                                className="rounded border border-gray-300 bg-white px-3 py-2 text-sm">
                                <option value="58">58 mm thermal</option>
                                <option value="80">80 mm thermal</option>
                                <option value="standard">A4 / PDF</option>
                            </select>
                            <button type="button" onClick={() => window.print()}
                                className="rounded bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-800">
                                Print receipt
                            </button>
                            <button type="button" onClick={() => setSelected(null)} aria-label="Close receipt details"
                                className="rounded border px-3 py-2 text-sm">Close</button>
                        </div>
                    </div>
                    <div className="receipt-print mx-auto bg-white text-black" data-paper-width={paperWidth}>
                        <header className="receipt-print-center">
                            <h1 className="receipt-store-name">Book Store</h1>
                            <p>Sales receipt</p>
                            <p>Receipt #{selected.id}</p>
                        </header>
                        <div className="receipt-print-meta">
                            <p>Date: {displayDate(selected.saleDate)}</p>
                            <p>Customer: {selected.customerName || "Walk-in Customer"}</p>
                            <p>Cashier: {selected.cashierName}</p>
                        </div>
                        <table className="receipt-print-table">
                            <thead>
                                <tr>
                                    <th scope="col">Book</th>
                                    <th scope="col">Qty</th>
                                    <th scope="col">Unit price</th>
                                    <th scope="col">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                {selected.items.map((item, index) => (
                                    <tr key={`${item.productId}-${index}`}>
                                        <td><span>{item.title}</span><small>{item.sku}</small></td>
                                        <td>{item.quantity}</td>
                                        <td>{money(item.unitPrice)}</td>
                                        <td>{money(item.subtotal)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                        <div className="receipt-print-total">
                            <p><span>Payment</span><strong>Cash</strong></p>
                            <p className="receipt-grand-total"><span>Grand total</span><strong>{money(selected.totalAmount)}</strong></p>
                        </div>
                        <footer className="receipt-print-center">
                            <p>Thank you for shopping with us!</p>
                            <p>Please keep this receipt for your records.</p>
                        </footer>
                    </div>
                </section>
            )}
        </div>
    );
}
