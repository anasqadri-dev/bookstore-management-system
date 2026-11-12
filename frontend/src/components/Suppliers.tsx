
"use client";

import { useState } from "react";

type Supplier = {
    id: number;
    name: string;
    contactPerson: string;
    phone: string;
    email: string;
    address: string;
};

export default function Suppliers() {
    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);

    const [form, setForm] = useState({
        name: "",
        contactPerson: "",
        phone: "",
        email: "",
        address: "",
    });

    function resetForm() {
        setForm({
            name: "",
            contactPerson: "",
            phone: "",
            email: "",
            address: "",
        });
        setEditingId(null);
        setShowForm(false);
    }

    function saveSupplier(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (!form.name.trim() || !form.phone.trim()) {
            alert("Supplier name and phone number are required.");
            return;
        }

        if (editingId !== null) {
            setSuppliers((previous) =>
                previous.map((supplier) =>
                    supplier.id === editingId
                        ? { ...supplier, ...form }
                        : supplier
                )
            );
        } else {
            const newSupplier: Supplier = {
                id: Date.now(),
                ...form,
            };

            setSuppliers((previous) => [...previous, newSupplier]);
        }

        resetForm();
    }

    function editSupplier(supplier: Supplier) {
        setForm({
            name: supplier.name,
            contactPerson: supplier.contactPerson,
            phone: supplier.phone,
            email: supplier.email,
            address: supplier.address,
        });
        setEditingId(supplier.id);
        setShowForm(true);
    }

    function deleteSupplier(id: number) {
        if (window.confirm("Are you sure you want to delete this supplier?")) {
            setSuppliers((previous) =>
                previous.filter((supplier) => supplier.id !== id)
            );
        }
    }

    const filteredSuppliers = suppliers.filter((supplier) =>
        [supplier.name, supplier.contactPerson, supplier.phone]
            .some((value) =>
                value.toLowerCase().includes(search.toLowerCase())
            )
    );

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">
                        Suppliers Management
                    </h2>
                    <p className="mt-1 text-sm text-gray-500">
                        Manage bookstore suppliers and their contact details.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={() => {
                        resetForm();
                        setShowForm(true);
                    }}
                    className="rounded bg-blue-600 px-5 py-3 text-white hover:bg-blue-700"
                >
                    + Add Supplier
                </button>
            </div>

            {showForm && (
                <form
                    onSubmit={saveSupplier}
                    className="space-y-4 rounded border border-gray-200 bg-white p-6"
                >
                    <h3 className="text-xl font-semibold text-gray-900">
                        {editingId !== null ? "Edit Supplier" : "Add New Supplier"}
                    </h3>

                    <div className="grid gap-4 md:grid-cols-2">
                        {[
                            { key: "name", label: "Supplier Name", required: true },
                            { key: "contactPerson", label: "Contact Person" },
                            { key: "phone", label: "Phone Number", required: true },
                            { key: "email", label: "Email Address" },
                        ].map((field) => (
                            <label key={field.key} className="block">
                <span className="mb-2 block text-sm font-medium text-gray-700">
                  {field.label}
                </span>
                                <input
                                    type={field.key === "email" ? "email" : "text"}
                                    required={field.required}
                                    value={form[field.key as keyof typeof form]}
                                    onChange={(e) =>
                                        setForm((previous) => ({
                                            ...previous,
                                            [field.key]: e.target.value,
                                        }))
                                    }
                                    className="w-full rounded border border-gray-300 px-3 py-3 text-gray-900"
                                    placeholder={`Enter ${field.label.toLowerCase()}`}
                                />
                            </label>
                        ))}
                    </div>

                    <label className="block">
            <span className="mb-2 block text-sm font-medium text-gray-700">
              Address
            </span>
                        <textarea
                            value={form.address}
                            onChange={(e) =>
                                setForm({ ...form, address: e.target.value })
                            }
                            rows={3}
                            className="w-full rounded border border-gray-300 px-3 py-3 text-gray-900"
                            placeholder="Enter supplier address"
                        />
                    </label>

                    <div className="flex gap-3">
                        <button
                            type="submit"
                            className="rounded bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                        >
                            {editingId !== null ? "Update Supplier" : "Save Supplier"}
                        </button>

                        <button
                            type="button"
                            onClick={resetForm}
                            className="rounded border border-gray-300 px-5 py-2 text-gray-800"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            <div className="rounded border border-gray-200 bg-white p-6">
                <label htmlFor="suppliers-search" className="sr-only">
                     Search suppliers by name, contact person, or phone number
                </label>
                <input
                    type="text"
                    id="suppliers-search"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Search by supplier name, contact person or phone..."
                    className="mb-5 w-full rounded border border-gray-300 px-4 py-3 text-gray-900"
                />

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100 text-gray-900">
                        <tr>
                            <th className="p-4">Supplier Name</th>
                            <th className="p-4">Contact Person</th>
                            <th className="p-4">Phone</th>
                            <th className="p-4">Email</th>
                            <th className="p-4">Address</th>
                            <th className="p-4">Actions</th>
                        </tr>
                        </thead>

                        <tbody>
                        {filteredSuppliers.map((supplier) => (
                            <tr
                                key={supplier.id}
                                className="border-b border-gray-200 text-gray-800"
                            >
                                <td className="p-4">{supplier.name}</td>
                                <td className="p-4">{supplier.contactPerson || "-"}</td>
                                <td className="p-4">{supplier.phone}</td>
                                <td className="p-4">{supplier.email || "-"}</td>
                                <td className="p-4">{supplier.address || "-"}</td>
                                <td className="p-4">
                                    <div className="flex gap-3">
                                        <button
                                            type="button"
                                            onClick={() => editSupplier(supplier)}
                                            className="text-blue-600 hover:underline"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => deleteSupplier(supplier.id)}
                                            className="text-red-600 hover:underline"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}

                        {filteredSuppliers.length === 0 && (
                            <tr>
                                <td
                                    colSpan={6}
                                    className="p-8 text-center text-gray-500"
                                >
                                    {search
                                        ? "No matching suppliers found."
                                        : "No suppliers added yet."}
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>

                <p className="mt-5 text-sm text-gray-600">
                    Total Suppliers: {suppliers.length}
                </p>
            </div>
        </div>
    );
}
