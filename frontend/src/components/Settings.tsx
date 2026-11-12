
"use client";

import { useState } from "react";

export default function Settings() {
    const initialSettings = {
        storeName: "Book Store",
        phone: "",
        email: "",
        address: "",
        adminName: "Admin",
        adminEmail: "",
        lowStockLimit: "5",
    };

    const [settings, setSettings] = useState(initialSettings);
    const [saved, setSaved] = useState(false);

    function handleChange(
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
    ) {
        const { name, value } = e.target;

        setSettings({
            ...settings,
            [name]: value,
        });

        setSaved(false);
    }

    function handleSave(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        // Frontend only: settings are not saved to a database yet.
        setSaved(true);
    }

    function handleReset() {
        setSettings(initialSettings);
        setSaved(false);
    }

    const inputStyle =
        "w-full rounded border border-gray-300 bg-white px-4 py-3 text-sm outline-none focus:border-blue-500";

    return (
        <form onSubmit={handleSave} className="space-y-8">

            {/* Store Information */}
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">
                    Store Information
                </h3>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Store Name
                        </label>

                        <input
                            type="text"
                            name="storeName"
                            required
                            value={settings.storeName}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Phone Number
                        </label>

                        <input
                            type="tel"
                            name="phone"
                            placeholder="Enter phone number"
                            value={settings.phone}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Email Address
                        </label>

                        <input
                            type="email"
                            name="email"
                            placeholder="Enter store email"
                            value={settings.email}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Store Address
                        </label>

                        <input
                            type="text"
                            name="address"
                            placeholder="Enter store address"
                            value={settings.address}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>
                </div>
            </section>

            {/* Admin Profile */}
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">
                    Admin Profile
                </h3>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Admin Name
                        </label>

                        <input
                            type="text"
                            name="adminName"
                            required
                            value={settings.adminName}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Admin Email
                        </label>

                        <input
                            type="email"
                            name="adminEmail"
                            placeholder="Enter admin email"
                            value={settings.adminEmail}
                            onChange={handleChange}
                            className={inputStyle}
                        />
                    </div>
                </div>
            </section>

            {/* Stock Settings */}
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-6 text-2xl font-semibold">
                    Stock Settings
                </h3>

                <div className="max-w-md">
                    <label className="mb-2 block text-sm font-medium">
                        Low Stock Alert Limit
                    </label>

                    <input
                        type="number"
                        name="lowStockLimit"
                        min="0"
                        step="1"
                        required
                        value={settings.lowStockLimit}
                        onChange={handleChange}
                        className={inputStyle}
                    />

                    <p className="mt-2 text-sm text-gray-500">
                        Books with stock at or below this limit
                        will be considered low stock once connected
                        to inventory data.
                    </p>
                </div>
            </section>

            {/* Action Buttons */}
            <div className="flex flex-wrap items-center gap-4">
                <button
                    type="submit"
                    className="rounded bg-blue-600 px-6 py-3 text-sm font-medium text-white hover:bg-blue-700"
                >
                    Save Settings
                </button>

                <button
                    type="button"
                    onClick={handleReset}
                    className="rounded border border-gray-300 bg-white px-6 py-3 text-sm hover:bg-gray-100"
                >
                    Reset
                </button>

                {saved && (
                    <p className="text-sm font-medium text-green-600">
                        Settings updated in this session!
                    </p>
                )}
            </div>
        </form>
    );
}
