
"use client";

import { useState } from "react";
import {
    LayoutDashboard,
    BookOpen,
    ShoppingCart,
    Truck,
    GraduationCap,
    BarChart3,
    Settings,
    UserRound,
} from "lucide-react";

export default function Home() {
    const [activePage, setActivePage] = useState("Dashboard");

    const menu = [
        { name: "Dashboard", icon: LayoutDashboard },
        { name: "Books Inventory", icon: BookOpen },
        { name: "Sales / POS", icon: ShoppingCart },
        { name: "Suppliers", icon: Truck },
        { name: "School Orders", icon: GraduationCap },
        { name: "Reports", icon: BarChart3 },
        { name: "Settings", icon: Settings },
    ];

    const stats = [
        { title: "Total Books", value: "0" },
        { title: "Today's Sales", value: "Rs. 0" },
        { title: "Low Stock", value: "0" },
        { title: "Suppliers", value: "0" },
    ];

    return (
        <div className="min-h-screen bg-[#eeeeee] text-black md:flex">

            {/* Sidebar */}
            <aside className="border-b border-gray-400 bg-[#eeeeee] p-5 md:min-h-screen md:w-64 md:shrink-0 md:border-b-0 md:border-r">
                <h1 className="text-2xl font-semibold">Qadri Books</h1>
                <p className="mt-1 text-sm">Management System</p>

                <nav className="mt-8 flex gap-2 overflow-x-auto md:block md:space-y-2">
                    {menu.map((item) => {
                        const Icon = item.icon;

                        return (
                            <button
                                key={item.name}
                                onClick={() => setActivePage(item.name)}
                                className={`flex shrink-0 items-center gap-3 rounded px-3 py-3 text-left text-sm md:w-full ${
                                    activePage === item.name
                                        ? "bg-blue-600 text-white"
                                        : "text-black hover:bg-gray-200"
                                }`}
                            >
                                <Icon size={18} />
                                {item.name}
                            </button>
                        );
                    })}
                </nav>
            </aside>

            {/* Main Content */}
            <main className="min-w-0 flex-1 p-5 md:p-8">
                <header className="mb-8 flex items-center justify-between gap-4">
                    <div>
                        <h2 className="text-3xl font-semibold">{activePage}</h2>
                        <p className="mt-2 text-sm">
                            Bookstore Management System
                        </p>
                    </div>

                    <div className="flex items-center gap-2 text-sm">
                        <UserRound size={18} />
                        Admin
                    </div>
                </header>

                {activePage === "Dashboard" ? (
                    <>
                        {/* Statistics */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                            {stats.map((stat) => (
                                <div
                                    key={stat.title}
                                    className="rounded border border-gray-400 bg-white p-5"
                                >
                                    <p className="text-sm">{stat.title}</p>
                                    <h3 className="mt-3 text-3xl font-semibold">
                                        {stat.value}
                                    </h3>
                                </div>
                            ))}
                        </div>

                        {/* Recent Sales */}
                        <section className="mt-6 rounded border border-gray-400 bg-white p-5">
                            <h3 className="mb-4 text-xl font-semibold">
                                Recent Sales
                            </h3>

                            <div className="overflow-x-auto">
                                <table className="w-full text-left text-sm">
                                    <thead className="bg-[#eeeeee]">
                                    <tr>
                                        <th className="border p-3">Book Name</th>
                                        <th className="border p-3">Quantity</th>
                                        <th className="border p-3">Amount</th>
                                        <th className="border p-3">Date</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    <tr>
                                        <td
                                            colSpan={4}
                                            className="border p-6 text-center"
                                        >
                                            No sales records available.
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </>
                ) : (
                    <section className="rounded border border-gray-400 bg-white p-6">
                        <h3 className="text-xl font-semibold">{activePage}</h3>
                        <p className="mt-3 text-sm">
                            This section is under development.
                        </p>
                    </section>
                )}
            </main>
        </div>
    );
}
