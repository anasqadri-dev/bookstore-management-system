"use client";

import { useCallback, useEffect, useState } from "react";
import { apiRequest } from "../lib/api";

type RecentSale = {
    id?: number;
    saleDate: string;
    customerName: string;
    cashierName: string;
    totalAmount: number;
};
type Summary = {
    totalBooks: number;
    todaySales: number;
    lowStock: number;
    suppliers: number;
    recentSales: RecentSale[];
};
const money = (value: number) => `Rs. ${value.toLocaleString("en-PK", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function validSummary(value: Summary): boolean {
    return !!value && [value.totalBooks, value.todaySales, value.lowStock, value.suppliers].every(
        (field) => typeof field === "number" && Number.isFinite(field)
    ) && Array.isArray(value.recentSales) && value.recentSales.every(
        (sale) => typeof sale.saleDate === "string"
            && typeof sale.customerName === "string"
            && typeof sale.cashierName === "string"
            && typeof sale.totalAmount === "number" && Number.isFinite(sale.totalAmount)
    );
}

export default function DashboardOverview() {
    const [summary, setSummary] = useState<Summary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [refreshKey, setRefreshKey] = useState(0);
    const refresh = useCallback(() => setRefreshKey((value) => value + 1), []);

    useEffect(() => {
        let active = true;
        apiRequest<Summary>("/api/dashboard", { cache: "no-store" })
            .then((data) => {
                if (!validSummary(data)) throw new Error("The server returned invalid dashboard data.");
                if (active) {
                    setSummary(data);
                    setError("");
                }
            })
            .catch((reason: unknown) => {
                if (active) {
                    setSummary(null);
                    setError(reason instanceof Error ? reason.message : "Unable to load dashboard.");
                }
            })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [refreshKey]);

    if (loading) return <p role="status" className="rounded bg-white p-6">Loading dashboard...</p>;
    if (error || !summary) return (
        <section role="alert" className="rounded border bg-white p-6">
            <p className="text-red-700">{error || "Dashboard data is unavailable."}</p>
            <button type="button" onClick={() => { setLoading(true); refresh(); }} className="mt-4 rounded bg-slate-800 px-4 py-2 text-white">Retry</button>
        </section>
    );

    const cards = [
        { title: "Total Books", value: summary.totalBooks.toLocaleString("en-PK") },
        { title: "Today's Sales", value: money(summary.todaySales) },
        { title: "Low Stock", value: summary.lowStock.toLocaleString("en-PK") },
        { title: "Suppliers", value: summary.suppliers.toLocaleString("en-PK") },
    ];
    return (
        <>
            <div className="mb-4 flex justify-end">
                <button type="button" onClick={() => { setLoading(true); refresh(); }} className="rounded border bg-white px-4 py-2 text-sm hover:bg-gray-50">Refresh dashboard</button>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                {cards.map((card) => (
                    <div key={card.title} className="rounded border border-gray-200 bg-white p-6">
                        <p className="text-gray-500">{card.title}</p>
                        <h3 className="mt-5 text-3xl font-bold">{card.value}</h3>
                    </div>
                ))}
            </div>
            <section className="mt-8 rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">Recent Sales</h3>
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100">
                            <tr><th className="p-4">Sale Date</th><th className="p-4">Customer</th><th className="p-4">Cashier</th><th className="p-4">Amount</th></tr>
                        </thead>
                        <tbody>
                            {summary.recentSales.length === 0 ? (
                                <tr><td colSpan={4} className="p-9 text-center text-gray-500">No sales records available.</td></tr>
                            ) : summary.recentSales.map((sale, index) => (
                                <tr key={sale.id ?? index} className="border-b">
                                    <td className="whitespace-nowrap p-4">{new Date(sale.saleDate).toLocaleString("en-PK")}</td>
                                    <td className="p-4">{sale.customerName}</td>
                                    <td className="p-4">{sale.cashierName}</td>
                                    <td className="whitespace-nowrap p-4">{money(sale.totalAmount)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </>
    );
}
