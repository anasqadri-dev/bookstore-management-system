
"use client";

import { useState } from "react";

export default function Reports() {
    const [reportType, setReportType] = useState("Sales");
    const [search, setSearch] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");

    const reports = [
        { name: "Sales", total: "Rs. 0", label: "Total Sales" },
        { name: "Inventory", total: "0", label: "Total Books" },
        { name: "Low Stock", total: "0", label: "Low Stock Books" },
        { name: "School Orders", total: "0", label: "Total Orders" },
    ];

    return (
        <div className="space-y-8">

            {/* Report Cards */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                {reports.map((report) => (
                    <div
                        key={report.name}
                        className="rounded border border-gray-200 bg-white p-6"
                    >
                        <p className="text-gray-500">
                            {report.label}
                        </p>

                        <h3 className="mt-5 text-3xl font-bold">
                            {report.total}
                        </h3>
                    </div>
                ))}
            </div>

            {/* Report Filters */}
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">
                    Generate Report
                </h3>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Report Type
                        </label>

                        <select
                            value={reportType}
                            onChange={(e) => setReportType(e.target.value)}
                            className="w-full rounded border border-gray-300 bg-white p-3"
                        >
                            <option>Sales</option>
                            <option>Inventory</option>
                            <option>Low Stock</option>
                            <option>School Orders</option>
                        </select>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Start Date
                        </label>

                        <input
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            className="w-full rounded border border-gray-300 p-3"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            End Date
                        </label>

                        <input
                            type="date"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            className="w-full rounded border border-gray-300 p-3"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Search
                        </label>

                        <input
                            type="text"
                            placeholder="Search records..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="w-full rounded border border-gray-300 p-3"
                        />
                    </div>
                </div>
            </section>

            {/* Report Table */}
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">
                    {reportType} Report
                </h3>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-[#f3f4f6]">
                        <tr>
                            <th className="p-4">Date</th>
                            <th className="p-4">Description</th>
                            <th className="p-4">Quantity</th>
                            <th className="p-4">Amount / Status</th>
                        </tr>
                        </thead>

                        <tbody>
                        <tr>
                            <td
                                colSpan={4}
                                className="p-9 text-center text-gray-500"
                            >
                                No {reportType.toLowerCase()} records available.
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
