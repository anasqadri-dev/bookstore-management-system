
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
    LibraryBig,
    Wallet,
    AlertTriangle,
    Package,
    type LucideIcon,
} from "lucide-react";

type MenuItem = {
    title: string;
    icon: LucideIcon;
};

type StatItem = {
    title: string;
    value: string;
    icon: LucideIcon;
    color: string;
    background: string;
};

export default function Home() {
    const [activePage, setActivePage] = useState("Dashboard");

    const menu: MenuItem[] = [
        { title: "Dashboard", icon: LayoutDashboard },
        { title: "Books Inventory", icon: BookOpen },
        { title: "Sales / POS", icon: ShoppingCart },
        { title: "Suppliers", icon: Truck },
        { title: "School Orders", icon: GraduationCap },
        { title: "Reports", icon: BarChart3 },
        { title: "Settings", icon: Settings },
    ];

    const stats: StatItem[] = [
        {
            title: "Total Books",
            value: "0",
            icon: LibraryBig,
            color: "text-blue-600",
            background: "bg-blue-100",
        },
        {
            title: "Today's Sales",
            value: "Rs. 0",
            icon: Wallet,
            color: "text-green-600",
            background: "bg-green-100",
        },
        {
            title: "Low Stock",
            value: "0",
            icon: AlertTriangle,
            color: "text-orange-600",
            background: "bg-orange-100",
        },
        {
            title: "Suppliers",
            value: "0",
            icon: Truck,
            color: "text-purple-600",
            background: "bg-purple-100",
        },
    ];

    return (
        <div className="flex min-h-screen flex-col bg-slate-100 text-slate-900 md:flex-row">

            {/* Sidebar */}
            <aside className="w-full shrink-0 bg-slate-900 p-6 text-white md:min-h-screen md:w-64">

                {/* Logo */}
                <div className="mb-8">
                    <div className="flex items-center gap-3">
                        <div className="rounded-xl bg-blue-600 p-3">
                            <LibraryBig size={26} className="text-white" />
                        </div>

                        <h1 className="text-xl font-bold">
                            Qadri Books
                        </h1>
                    </div>

                    <p className="mt-3 text-sm text-slate-400">
                        Management System
                    </p>
                </div>

                {/* Navigation */}
                <nav className="space-y-2">
                    {menu.map((item) => {
                        const Icon = item.icon;

                        return (
                            <button
                                key={item.title}
                                type="button"
                                onClick={() => setActivePage(item.title)}
                                className={`flex w-full items-center gap-3 rounded-lg px-4 py-3 text-left text-sm font-medium transition ${
                                    activePage === item.title
                                        ? "bg-blue-600 text-white shadow-md"
                                        : "text-slate-300 hover:bg-slate-800 hover:text-white"
                                }`}
                            >
                                <Icon size={20} strokeWidth={2} />
                                <span>{item.title}</span>
                            </button>
                        );
                    })}
                </nav>

                {/* Sidebar Footer */}
                <div className="mt-10 border-t border-slate-700 pt-5">
                    <p className="text-xs text-slate-400">
                        Qadri Books Management
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                        Version 1.0
                    </p>
                </div>
            </aside>

            {/* Main Content */}
            <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">

                {/* Header */}
                <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <h2 className="text-2xl font-bold sm:text-3xl">
                            {activePage}
                        </h2>

                        <p className="mt-2 text-sm text-slate-500 sm:text-base">
                            Welcome to Bookstore Management System
                        </p>
                    </div>

                    {/* Admin Profile */}
                    <div className="flex items-center gap-3 rounded-full bg-white px-5 py-3 shadow-sm">
                        <UserRound size={20} className="text-blue-600" />
                        <span className="font-medium">Admin</span>
                    </div>
                </header>

                {/* Dashboard */}
                {activePage === "Dashboard" ? (
                    <>
                        {/* Statistics Cards */}
                        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                            {stats.map((stat) => {
                                const Icon = stat.icon;

                                return (
                                    <div
                                        key={stat.title}
                                        className="rounded-xl bg-white p-6 shadow-sm transition-shadow hover:shadow-md"
                                    >
                                        <div
                                            className={`mb-4 flex h-12 w-12 items-center justify-center rounded-xl ${stat.background}`}
                                        >
                                            <Icon size={25} className={stat.color} />
                                        </div>

                                        <p className="text-sm text-slate-500">
                                            {stat.title}
                                        </p>

                                        <h3 className="mt-2 text-3xl font-bold">
                                            {stat.value}
                                        </h3>
                                    </div>
                                );
                            })}
                        </div>

                        {/* Recent Sales */}
                        <section className="mt-8 rounded-xl bg-white p-6 shadow-sm">
                            <div className="mb-5 flex items-center gap-3">
                                <ShoppingCart
                                    size={23}
                                    className="text-blue-600"
                                />

                                <h3 className="text-xl font-semibold">
                                    Recent Sales
                                </h3>
                            </div>

                            <div className="flex flex-col items-center justify-center py-10 text-center">
                                <div className="mb-4 rounded-full bg-slate-100 p-5">
                                    <Package
                                        size={32}
                                        className="text-slate-400"
                                    />
                                </div>

                                <p className="font-medium text-slate-700">
                                    No Sales Yet
                                </p>

                                <p className="mt-2 text-sm text-slate-500">
                                    Your recent sales will appear here.
                                </p>
                            </div>
                        </section>
                    </>
                ) : (
                    /* Other Modules */
                    <section className="rounded-xl bg-white p-8 shadow-sm">
                        <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-xl bg-blue-100">
                            {(() => {
                                const selected = menu.find(
                                    (item) => item.title === activePage
                                );
                                const Icon = selected?.icon ?? BookOpen;

                                return (
                                    <Icon size={28} className="text-blue-600" />
                                );
                            })()}
                        </div>

                        <h3 className="text-xl font-semibold">
                            {activePage}
                        </h3>

                        <p className="mt-3 text-slate-500">
                            This module will be developed in the next step.
                        </p>
                    </section>
                )}
            </main>
        </div>
    );
}
