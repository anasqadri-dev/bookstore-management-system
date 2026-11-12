
"use client";

import { useState } from "react";

type Sale = {
    id: string;
    date: string;
    customer: string;
    book: string;
    quantity: number;
    total: number;
    payment: "Cash" | "Card" | "Online";
};

export default function SalesHistory() {
    const [sales] = useState<Sale[]>([]);
    const [search, setSearch] = useState("");
    const [date, setDate] = useState("");
    const [selected, setSelected] = useState<Sale | null>(null);

    const filtered = sales.filter((sale) => {
        const matchSearch =
            `${sale.id} ${sale.customer} ${sale.book}`
                .toLowerCase()
                .includes(search.toLowerCase());

        return matchSearch && (!date || sale.date === date);
    });

    const totalRevenue = filtered.reduce(
        (sum, sale) => sum + sale.total, 0
    );

    const totalBooks = filtered.reduce(
        (sum, sale) => sum + sale.quantity, 0
    );

    const money = (value: number) =>
        `Rs. ${value.toLocaleString()}`;

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h3 className="text-xl font-semibold">Sales Overview</h3>
                    <p className="mt-1 text-sm text-gray-500">
                        View and track bookstore transactions.
                    </p>
                </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
                {[
                    { label: "Total Sales", value: filtered.length },
                    { label: "Revenue", value: money(totalRevenue) },
                    { label: "Books Sold", value: totalBooks },
                ].map((card) => (
                    <div
                        key={card.label}
                        className="rounded-lg border border-gray-200 bg-white p-6"
                    >
                        <p className="text-sm text-gray-500">{card.label}</p>
                        <h3 className="mt-3 text-2xl font-bold">{card.value}</h3>
                    </div>
                ))}
            </div>

            <div className="rounded-lg border border-gray-200 bg-white p-6">
                <div className="mb-5 flex flex-wrap gap-3">
                    <input
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Search sales..."
                        className="min-w-0 flex-1 rounded-md border border-gray-300 px-4 py-3 text-sm outline-none focus:border-slate-600"
                    />
                    <input
                        type="date"
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                        className="rounded-md border border-gray-300 px-4 py-3 text-sm"
                    />
                    <button
                        onClick={() => {
                            setSearch("");
                            setDate("");
                        }}
                        className="rounded-md border px-4 py-3 text-sm hover:bg-gray-50"
                    >
                        Clear
                    </button>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100 text-gray-600">
                        <tr>
                            {["Sale ID", "Date", "Customer", "Book",
                                "Qty", "Total", "Payment", "Action"].map((h) => (
                                <th key={h} className="whitespace-nowrap p-4">{h}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        {filtered.length === 0 ? (
                            <tr>
                                <td colSpan={8} className="p-12 text-center text-gray-500">
                                    No sales records available.
                                </td>
                            </tr>
                        ) : filtered.map((sale) => (
                            <tr key={sale.id} className="border-b">
                                <td className="p-4 font-medium">{sale.id}</td>
                                <td className="p-4">{sale.date}</td>
                                <td className="p-4">{sale.customer}</td>
                                <td className="p-4">{sale.book}</td>
                                <td className="p-4">{sale.quantity}</td>
                                <td className="p-4">{money(sale.total)}</td>
                                <td className="p-4">{sale.payment}</td>
                                <td className="p-4">
                                    <button
                                        onClick={() => setSelected(sale)}
                                        className="font-medium text-blue-700 hover:underline"
                                    >
                                        View
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {selected && (
                <div className="rounded-lg border bg-white p-6">
                    <div className="flex justify-between">
                        <h3 className="text-lg font-semibold">Sale Details</h3>
                        <button onClick={() => setSelected(null)}>Close</button>
                    </div>
                    <div className="mt-4 space-y-2 text-sm">
                        <p>Sale ID: {selected.id}</p>
                        <p>Date: {selected.date}</p>
                        <p>Customer: {selected.customer}</p>
                        <p>Book: {selected.book}</p>
                        <p>Quantity: {selected.quantity}</p>
                        <p>Payment: {selected.payment}</p>
                        <p className="font-bold">Total: {money(selected.total)}</p>
                    </div>
                </div>
            )}
        </div>
    );
}
