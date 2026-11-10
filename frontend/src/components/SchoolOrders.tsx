
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
            setOrders(
                orders.map((order) =>
                    order.id === editingId
                        ? { ...form, id: editingId, quantity, price }
                        : order
                )
            );
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

    const filteredOrders = orders.filter(
        (order) =>
            order.schoolName.toLowerCase().includes(search.toLowerCase()) ||
            order.bookName.toLowerCase().includes(search.toLowerCase())
    );

    const totalAmount = orders.reduce(
        (total, order) => total + order.quantity * order.price,
        0
    );

    const pendingOrders = orders.filter(
        (order) => order.status === "Pending"
    ).length;

    const inputStyle =
        "w-full rounded border border-gray-300 bg-white px-4 py-3 text-sm outline-none focus:border-blue-500";

    return (
        <div className="space-y-8">

            {/* Add Order Button */}
            <div className="flex flex-wrap items-center justify-between gap-4">
                <p className="text-sm text-gray-500">
                    Manage book orders received from schools.
                </p>

                <button
                    type="button"
                    onClick={() => {
                        resetForm();
                        setShowForm(true);
                    }}
                    className="rounded bg-blue-600 px-5 py-3 text-sm font-medium text-white hover:bg-blue-700"
                >
                    + Add School Order
                </button>
            </div>

            {/* Statistics Cards */}
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">

                <div className="rounded border border-gray-200 bg-white p-6">
                    <p className="text-gray-500">
                        Total Orders
                    </p>

                    <h3 className="mt-5 text-3xl font-bold">
                        {orders.length}
                    </h3>
                </div>

                <div className="rounded border border-gray-200 bg-white p-6">
                    <p className="text-gray-500">
                        Pending Orders
                    </p>

                    <h3 className="mt-5 text-3xl font-bold">
                        {pendingOrders}
                    </h3>
                </div>

                <div className="rounded border border-gray-200 bg-white p-6">
                    <p className="text-gray-500">
                        Total Amount
                    </p>

                    <h3 className="mt-5 text-3xl font-bold">
                        Rs. {totalAmount.toLocaleString()}
                    </h3>
                </div>
            </div>

            {/* Add / Edit Order Form */}
            {showForm && (
                <form
                    onSubmit={saveOrder}
                    className="rounded border border-gray-200 bg-white p-6"
                >
                    <h3 className="mb-6 text-xl font-semibold">
                        {editingId !== null
                            ? "Edit School Order"
                            : "Add School Order"}
                    </h3>

                    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                School Name
                            </label>

                            <input
                                required
                                placeholder="Enter school name"
                                value={form.schoolName}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        schoolName: e.target.value,
                                    })
                                }
                                className={inputStyle}
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                Contact Number
                            </label>

                            <input
                                required
                                placeholder="Enter contact number"
                                value={form.contact}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        contact: e.target.value,
                                    })
                                }
                                className={inputStyle}
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                Book Name
                            </label>

                            <input
                                required
                                placeholder="Enter book name"
                                value={form.bookName}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        bookName: e.target.value,
                                    })
                                }
                                className={inputStyle}
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                Quantity
                            </label>

                            <input
                                required
                                type="number"
                                min="1"
                                step="1"
                                placeholder="Enter quantity"
                                value={form.quantity}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        quantity: e.target.value,
                                    })
                                }
                                className={inputStyle}
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                Price per Book (Rs.)
                            </label>

                            <input
                                required
                                type="number"
                                min="0"
                                step="0.01"
                                placeholder="Enter price"
                                value={form.price}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        price: e.target.value,
                                    })
                                }
                                className={inputStyle}
                            />
                        </div>

                        <div>
                            <label className="mb-2 block text-sm font-medium">
                                Order Status
                            </label>

                            <select
                                value={form.status}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        status: e.target.value as SchoolOrder["status"],
                                    })
                                }
                                className={inputStyle}
                            >
                                <option value="Pending">Pending</option>
                                <option value="Processing">Processing</option>
                                <option value="Delivered">Delivered</option>
                            </select>
                        </div>
                    </div>

                    <div className="mt-6 flex flex-wrap gap-3">
                        <button
                            type="submit"
                            className="rounded bg-blue-600 px-5 py-3 text-sm font-medium text-white hover:bg-blue-700"
                        >
                            {editingId !== null
                                ? "Update Order"
                                : "Save Order"}
                        </button>

                        <button
                            type="button"
                            onClick={resetForm}
                            className="rounded border border-gray-300 bg-white px-5 py-3 text-sm hover:bg-gray-100"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {/* School Orders Table */}
            <section className="rounded border border-gray-200 bg-white p-6">

                <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
                    <h3 className="text-2xl font-semibold">
                        School Orders List
                    </h3>

                    <input
                        type="text"
                        placeholder="Search by school or book name..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full rounded border border-gray-300 px-4 py-3 text-sm outline-none focus:border-blue-500 sm:w-72"
                    />
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-[#f3f4f6]">
                        <tr>
                            <th className="p-4">School</th>
                            <th className="p-4">Contact</th>
                            <th className="p-4">Book</th>
                            <th className="p-4">Quantity</th>
                            <th className="p-4">Total (Rs.)</th>
                            <th className="p-4">Status</th>
                            <th className="p-4">Actions</th>
                        </tr>
                        </thead>

                        <tbody>
                        {filteredOrders.map((order) => (
                            <tr
                                key={order.id}
                                className="border-b border-gray-200"
                            >
                                <td className="p-4">
                                    {order.schoolName}
                                </td>

                                <td className="p-4">
                                    {order.contact}
                                </td>

                                <td className="p-4">
                                    {order.bookName}
                                </td>

                                <td className="p-4">
                                    {order.quantity}
                                </td>

                                <td className="p-4">
                                    {(order.quantity * order.price).toLocaleString()}
                                </td>

                                <td className="p-4">
                                        <span
                                            className={`inline-block rounded px-3 py-1 text-xs font-medium ${
                                                order.status === "Pending"
                                                    ? "bg-yellow-100 text-yellow-800"
                                                    : order.status === "Processing"
                                                        ? "bg-blue-100 text-blue-800"
                                                        : "bg-green-100 text-green-800"
                                            }`}
                                        >
                                            {order.status}
                                        </span>
                                </td>

                                <td className="p-4">
                                    <div className="flex items-center gap-4">
                                        <button
                                            type="button"
                                            onClick={() => editOrder(order)}
                                            className="font-medium text-blue-600 hover:underline"
                                        >
                                            Edit
                                        </button>

                                        <button
                                            type="button"
                                            onClick={() => deleteOrder(order.id)}
                                            className="font-medium text-red-600 hover:underline"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}

                        {filteredOrders.length === 0 && (
                            <tr>
                                <td
                                    colSpan={7}
                                    className="p-9 text-center text-gray-500"
                                >
                                    No school orders found.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
