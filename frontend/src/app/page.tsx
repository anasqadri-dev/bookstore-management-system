
"use client";

import { useState } from "react";
import BooksInventory from "../components/BooksInventory";
import SalesPOS from "../components/SalesPOS";
import Suppliers from "../components/Suppliers";
import SchoolOrders from "../components/SchoolOrders";

export default function Home() {
    const [activePage, setActivePage] = useState("Dashboard");

    const menu = [
        { name: "Dashboard", icon: "▦" },
        { name: "Books Inventory", icon: "▤" },
        { name: "Sales / POS", icon: "🛒" },
        { name: "Suppliers", icon: "▱" },
        { name: "School Orders", icon: "♧" },
        { name: "Reports", icon: "▥" },
        { name: "Settings", icon: "⚙" },
    ];

    const stats = [
        { title: "Total Books", value: "0" },
        { title: "Today's Sales", value: "Rs. 0" },
        { title: "Low Stock", value: "0" },
        { title: "Suppliers", value: "0" },
    ];

    return (
        <div className="min-h-screen bg-[#f3f4f6] text-[#111827] md:flex md:h-screen md:overflow-hidden">

            {/* Sidebar */}
            <aside className="bg-[#030712] p-6 text-white md:h-screen md:w-[320px] md:shrink-0 md:overflow-y-auto">
                <h1 className="text-2xl font-bold">Book Store</h1>

                <p className="mt-2 text-sm text-gray-400">
                    Management System
                </p>

                <nav className="mt-10 space-y-2">
                    {menu.map((item) => (
                        <button
                            key={item.name}
                            type="button"
                            onClick={() => setActivePage(item.name)}
                            className={`flex w-full items-center gap-4 rounded-md px-5 py-4 text-left text-sm transition ${
                                activePage === item.name
                                    ? "bg-slate-700 text-white"
                                    : "text-gray-300 hover:bg-slate-800"
                            }`}
                        >
                            <span className="w-5 text-xl">{item.icon}</span>
                            {item.name}
                        </button>
                    ))}
                </nav>
            </aside>

            {/* Main Content */}
            <main className="min-w-0 flex-1 p-6 md:h-screen md:overflow-y-auto md:p-10">

                {/* Header */}
                <header className="mb-10 flex items-center justify-between gap-4">
                    <div>
                        <h2 className="text-3xl font-bold">
                            {activePage}
                        </h2>

                        <p className="mt-2 text-gray-500">
                            Bookstore Management System
                        </p>
                    </div>

                    <div className="shrink-0 text-sm">
                        ♙ &nbsp; Admin
                    </div>
                </header>

                {activePage === "Dashboard" ? (
                    <>
                        {/* Statistics Cards */}
                        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                            {stats.map((stat) => (
                                <div
                                    key={stat.title}
                                    className="rounded border border-gray-200 bg-white p-6"
                                >
                                    <p className="text-gray-500">
                                        {stat.title}
                                    </p>

                                    <h3 className="mt-5 text-3xl font-bold">
                                        {stat.value}
                                    </h3>
                                </div>
                            ))}
                        </div>

                        {/* Recent Sales */}
                        <section className="mt-8 rounded border border-gray-200 bg-white p-6">
                            <h3 className="mb-6 text-2xl font-semibold">
                                Recent Sales
                            </h3>

                            <div className="overflow-x-auto">
                                <table className="w-full text-left text-sm">
                                    <thead className="bg-[#f3f4f6]">
                                    <tr>
                                        <th className="p-4">Book Name</th>
                                        <th className="p-4">Quantity</th>
                                        <th className="p-4">Amount</th>
                                        <th className="p-4">Date</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    <tr>
                                        <td
                                            colSpan={4}
                                            className="p-9 text-center text-gray-500"
                                        >
                                            No sales records available.
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </>
                ) : activePage === "Books Inventory" ? (
                    <BooksInventory />
                ) : activePage === "Suppliers" ? (
                    <Suppliers />
                ) : activePage === "School Orders" ? (
                    <SchoolOrders />
                ) : activePage === "Sales / POS" ? (
                    <SalesPOS />
                ) : (
                    <section className="rounded border border-gray-200 bg-white p-6">
                        <h3 className="text-xl font-semibold">
                            {activePage}
                        </h3>

                        <p className="mt-3 text-gray-500">
                            This section is under development.
                        </p>
                    </section>
                )}
            </main>
        </div>
    );
}
