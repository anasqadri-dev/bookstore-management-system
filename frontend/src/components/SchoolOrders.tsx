
"use client";

import { useState } from "react";

type SchoolOrder = {
    id: number;
    schoolName: string;
    contact: string;
    bookName: string;
    quantity: number;
    price: number;
    status: "Pending" | "Processing" | "Delivered";
};

export default function SchoolOrders() {
    const [orders, setOrders] = useState<SchoolOrder[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);

    const [form, setForm] = useState({
        schoolName: "",
        contact: "",
        bookName: "",
        quantity: "",
        price: "",
        status: "Pending" as SchoolOrder["status"],
    });

    function resetForm() {
        setForm({
            schoolName: "",
            contact: "",
            bookName: "",
            quantity: "",
            price: "",
            status: "Pending",
        });
        setEditingId(null);
        setShowForm(false);
    }

    function saveOrder(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        const quantity = Number(form.quantity);
        const price = Number(form.price);

        if (quantity <= 0 || price < 0) {
            alert("Please enter valid quantity and price.");
            return;
        }

        if (editingId !== null) {
            setOrders(orders.map((order) =>
                order.id === editingId
                    ? { ...form, id: editingId, quantity, price }
                    : order
            ));
        } else {
            const newOrder: SchoolOrder = {
                ...form,
                id: Date.now(),
                quantity,
                price,
            };

            setOrders([...orders, newOrder]);
        }

        resetForm();
    }

    function editOrder(order: SchoolOrder) {
        setForm({
            schoolName: order.schoolName,
            contact: order.contact,
            bookName: order.bookName,
            quantity: String(order.quantity),
            price: String(order.price),
            status: order.status,
        });

        setEditingId(order.id);
        setShowForm(true);
    }

    function deleteOrder(id: number) {
        if (window.confirm("Delete this school order?")) {
            setOrders(orders.filter((order) => order.id !== id));
        }
    }

    const filteredOrders = orders.filter((order) =>
        order.schoolName.toLowerCase().includes(search.toLowerCase()) ||
        order.bookName.toLowerCase().includes(search.toLowerCase())
    );

    const totalAmount = orders.reduce(
        (total, order) => total + order.quantity * order.price,
        0
    );

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-bold">School Orders</h2>
                    <p className="text-sm text-gray-500">
                        Manage book orders received from schools.
                    </p>
                </div>

                <button
                    onClick={() => {
                        resetForm();
                        setShowForm(true);
                    }}
                    className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
                >
                    + Add School Order
                </button>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
                <div className="rounded border bg-white p-4">
                    <p className="text-sm text-gray-500">Total Orders</p>
                    <p className="text-2xl font-bold">{orders.length}</p>
                </div>

                <div className="rounded border bg-white p-4">
                    <p className="text-sm text-gray-500">Pending Orders</p>
                    <p className="text-2xl font-bold">
                        {orders.filter((order) => order.status === "Pending").length}
                    </p>
                </div>

                <div className="rounded border bg-white p-4">
                    <p className="text-sm text-gray-500">Total Amount</p>
                    <p className="text-2xl font-bold">Rs. {totalAmount}</p>
                </div>
            </div>

            {showForm && (
                <form
                    onSubmit={saveOrder}
                    className="space-y-4 rounded border bg-white p-5"
                >
                    <h3 className="text-lg font-semibold">
                        {editingId !== null ? "Edit School Order" : "Add School Order"}
                    </h3>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <input
                            required
                            placeholder="School Name"
                            value={form.schoolName}
                            onChange={(e) => setForm({ ...form, schoolName: e.target.value })}
                            className="rounded border p-2"
                        />

                        <input
                            required
                            placeholder="Contact Number"
                            value={form.contact}
                            onChange={(e) => setForm({ ...form, contact: e.target.value })}
                            className="rounded border p-2"
                        />

                        <input
                            required
                            placeholder="Book Name"
                            value={form.bookName}
                            onChange={(e) => setForm({ ...form, bookName: e.target.value })}
                            className="rounded border p-2"
                        />

                        <input
                            required
                            type="number"
                            min="1"
                            placeholder="Quantity"
                            value={form.quantity}
                            onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                            className="rounded border p-2"
                        />

                        <input
                            required
                            type="number"
                            min="0"
                            step="0.01"
                            placeholder="Price per Book (Rs.)"
                            value={form.price}
                            onChange={(e) => setForm({ ...form, price: e.target.value })}
                            className="rounded border p-2"
                        />

                        <select
                            value={form.status}
                            onChange={(e) =>
                                setForm({
                                    ...form,
                                    status: e.target.value as SchoolOrder["status"],
                                })
                            }
                            className="rounded border p-2"
                        >
                            <option value="Pending">Pending</option>
                            <option value="Processing">Processing</option>
                            <option value="Delivered">Delivered</option>
                        </select>
                    </div>

                    <div className="flex gap-3">
                        <button
                            type="submit"
                            className="rounded bg-green-600 px-4 py-2 text-white"
                        >
                            {editingId !== null ? "Update Order" : "Save Order"}
                        </button>

                        <button
                            type="button"
                            onClick={resetForm}
                            className="rounded border px-4 py-2"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            <div className="rounded border bg-white p-5">
                <input
                    placeholder="Search by school or book name..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="mb-4 w-full rounded border p-2"
                />

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="border-b bg-gray-100">
                        <tr>
                            <th className="p-3">School</th>
                            <th className="p-3">Contact</th>
                            <th className="p-3">Book</th>
                            <th className="p-3">Quantity</th>
                            <th className="p-3">Total (Rs.)</th>
                            <th className="p-3">Status</th>
                            <th className="p-3">Actions</th>
                        </tr>
                        </thead>

                        <tbody>
                        {filteredOrders.map((order) => (
                            <tr key={order.id} className="border-b">
                                <td className="p-3">{order.schoolName}</td>
                                <td className="p-3">{order.contact}</td>
                                <td className="p-3">{order.bookName}</td>
                                <td className="p-3">{order.quantity}</td>
                                <td className="p-3">{order.quantity * order.price}</td>
                                <td className="p-3">{order.status}</td>
                                <td className="p-3">
                                    <div className="flex gap-3">
                                        <button
                                            onClick={() => editOrder(order)}
                                            className="text-blue-600"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => deleteOrder(order.id)}
                                            className="text-red-600"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}

                        {filteredOrders.length === 0 && (
                            <tr>
                                <td colSpan={7} className="p-6 text-center text-gray-500">
                                    No school orders found.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
