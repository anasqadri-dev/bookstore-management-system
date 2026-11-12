"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";

type Supplier = {
    id: number;
    name: string;
    phone: string | null;
    address: string | null;
};

export default function Suppliers() {
    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState({ name: "", phone: "", address: "" });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [formError, setFormError] = useState("");
    const [notice, setNotice] = useState("");

    const loadSuppliers = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await apiRequest<Supplier[]>("/api/suppliers");
            setSuppliers(data);
        } catch {
            setError("Could not load suppliers. Check your connection and permissions, then retry.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        void loadSuppliers();
    }, [loadSuppliers]);

    async function saveSupplier(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (saving) return;

        if (!form.name.trim()) {
            setFormError("Supplier name is required.");
            return;
        }

        setSaving(true);
        setFormError("");
        setNotice("");
        try {
            await apiRequest<Supplier>("/api/suppliers", {
                method: "POST",
                body: JSON.stringify({
                    name: form.name.trim(),
                    phone: form.phone.trim() || null,
                    address: form.address.trim() || null,
                }),
            });
            setForm({ name: "", phone: "", address: "" });
            setShowForm(false);
            setNotice("Supplier saved successfully.");
            await loadSuppliers();
        } catch {
            setFormError("Could not save supplier. Check the entered details and your connection, then retry.");
        } finally {
            setSaving(false);
        }
    }

    const filteredSuppliers = suppliers.filter((supplier) =>
        [supplier.name, supplier.phone ?? "", supplier.address ?? ""].some((value) =>
            value.toLowerCase().includes(search.toLowerCase())
        )
    );

    const inputStyle = "w-full rounded border border-gray-300 bg-white px-3 py-3 text-gray-900";

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">Suppliers Management</h2>
                    <p className="mt-1 text-sm text-gray-500">
                        View and register suppliers. Editing and deleting suppliers are not supported by the current API.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => {
                        setFormError("");
                        setNotice("");
                        setShowForm((previous) => !previous);
                    }}
                    className="rounded bg-blue-600 px-5 py-3 text-white hover:bg-blue-700"
                >
                    {showForm ? "Cancel" : "+ Add Supplier"}
                </button>
            </div>

            {notice && <p role="status" className="rounded border border-green-200 bg-green-50 p-3 text-sm text-green-800">{notice}</p>}

            {showForm && (
                <form onSubmit={(event) => void saveSupplier(event)}
                    aria-busy={saving}
                    className="space-y-4 rounded border border-gray-200 bg-white p-6">
                    <h3 className="text-xl font-semibold text-gray-900">Add New Supplier</h3>
                    {formError && <p id="supplier-form-error" role="alert" className="text-sm text-red-700">{formError}</p>}
                    <div className="grid gap-4 md:grid-cols-2">
                        <div>
                            <label htmlFor="supplier-name" className="mb-2 block text-sm font-medium">Supplier Name</label>
                            <input id="supplier-name" required value={form.name}
                                aria-invalid={Boolean(formError)}
                                aria-describedby={formError ? "supplier-form-error" : undefined}
                                onChange={(event) => setForm({ ...form, name: event.target.value })}
                                className={inputStyle} disabled={saving} />
                        </div>
                        <div>
                            <label htmlFor="supplier-phone" className="mb-2 block text-sm font-medium">Phone Number (optional)</label>
                            <input id="supplier-phone" type="tel" value={form.phone}
                                onChange={(event) => setForm({ ...form, phone: event.target.value })}
                                className={inputStyle} disabled={saving} />
                        </div>
                    </div>
                    <div>
                        <label htmlFor="supplier-address" className="mb-2 block text-sm font-medium">Address (optional)</label>
                        <textarea id="supplier-address" value={form.address} rows={3}
                            onChange={(event) => setForm({ ...form, address: event.target.value })}
                            className={inputStyle} disabled={saving} />
                    </div>
                    <div className="flex flex-wrap gap-3">
                        <button type="submit" disabled={saving}
                            className="rounded bg-blue-600 px-5 py-2 text-white hover:bg-blue-700 disabled:opacity-50">
                            {saving ? "Saving..." : "Save Supplier"}
                        </button>
                        <button type="button" disabled={saving} onClick={() => { setShowForm(false); setFormError(""); }}
                            className="rounded border border-gray-300 px-5 py-2 disabled:opacity-50">
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            <section className="rounded border border-gray-200 bg-white p-6">
                <label htmlFor="suppliers-search" className="sr-only">
                    Search suppliers by name, phone, or address
                </label>
                <input id="suppliers-search" type="search" value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search suppliers..."
                    className="mb-5 w-full rounded border border-gray-300 px-4 py-3 text-gray-900" />

                {loading ? (
                    <p role="status" className="py-8 text-center text-gray-600">Loading suppliers...</p>
                ) : error ? (
                    <div role="alert" className="space-y-3 py-6 text-center">
                        <p className="text-red-700">{error}</p>
                        <button type="button" onClick={() => void loadSuppliers()}
                            className="rounded border border-gray-300 px-4 py-2">Retry</button>
                    </div>
                ) : (
                    <>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-gray-100 text-gray-900">
                                    <tr>
                                        <th scope="col" className="p-4">Supplier Name</th>
                                        <th scope="col" className="p-4">Phone</th>
                                        <th scope="col" className="p-4">Address</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredSuppliers.map((supplier) => (
                                        <tr key={supplier.id} className="border-b border-gray-200 text-gray-800">
                                            <td className="p-4">{supplier.name}</td>
                                            <td className="p-4">{supplier.phone || "-"}</td>
                                            <td className="p-4">{supplier.address || "-"}</td>
                                        </tr>
                                    ))}
                                    {filteredSuppliers.length === 0 && (
                                        <tr><td colSpan={3} className="p-8 text-center text-gray-500">
                                            {search ? "No matching suppliers found." : "No suppliers registered yet."}
                                        </td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        <p className="mt-5 text-sm text-gray-600">Total Suppliers: {suppliers.length}</p>
                    </>
                )}
            </section>
        </div>
    );
}
