
"use client";

import { useState } from "react";

type OrderItem = {
    book: string;
    quantity: number;
    price: number;
};

type PurchaseOrder = {
    id: string;
    supplier: string;
    date: string;
    status: "Pending" | "Ordered" | "Received" | "Cancelled";
    items: OrderItem[];
};

export default function PurchaseOrders() {
    const [orders, setOrders] = useState<PurchaseOrder[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [selected, setSelected] = useState<PurchaseOrder | null>(null);

    const [supplier, setSupplier] = useState("");
    const [items, setItems] = useState<OrderItem[]>([
        { book: "", quantity: 1, price: 0 },
    ]);

    const money = (value: number) =>
        `Rs. ${value.toLocaleString()}`;

    const calculateTotal = (list: OrderItem[]) =>
        list.reduce((sum, item) => sum + item.quantity * item.price, 0);

    function updateItem(
        index: number,
        field: keyof OrderItem,
        value: string
    ) {
        setItems((previous) =>
            previous.map((item, i) =>
                i === index
                    ? {
                        ...item,
                        [field]: field === "book" ? value : Number(value),
                    }
                    : item
            )
        );
    }

    function resetForm() {
        setSupplier("");
        setItems([{ book: "", quantity: 1, price: 0 }]);
        setShowForm(false);
    }

    function saveOrder(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (!supplier.trim() || items.some(
            (item) =>
                !item.book.trim() ||
                item.quantity <= 0 ||
                item.price < 0 ||
                !Number.isFinite(item.quantity) ||
                !Number.isFinite(item.price)
        )) {
            alert("Please enter valid supplier and book details.");
            return;
        }

        const newOrder: PurchaseOrder = {
            id: `PO-${Date.now()}`,
            supplier: supplier.trim(),
            date: new Date().toLocaleDateString("en-CA"),
            status: "Pending",
            items: items.map((item) => ({ ...item })),
        };

        setOrders((previous) => [newOrder, ...previous]);
        resetForm();
    }

    function changeStatus(
        id: string,
        status: PurchaseOrder["status"]
    ) {
        setOrders((previous) =>
            previous.map((order) =>
                order.id === id ? { ...order, status } : order
            )
        );
        setSelected((previous) =>
            previous?.id === id ? { ...previous, status } : previous
        );
    }

    const filtered = orders.filter((order) =>
        `${order.id} ${order.supplier} ${order.status}`
            .toLowerCase()
            .includes(search.toLowerCase())
    );

    const pending = orders.filter(
        (order) => order.status === "Pending"
    ).length;

    const received = orders.filter(
        (order) => order.status === "Received"
    ).length;

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h3 className="text-xl font-semibold">Purchase Management</h3>
                    <p className="mt-1 text-sm text-gray-500">
                        Manage book purchases from suppliers.
                    </p>
                </div>
                <button
                    onClick={() => setShowForm(!showForm)}
                    className="rounded-md bg-slate-900 px-5 py-3 text-sm font-medium text-white hover:bg-slate-800"
                >
                    {showForm ? "Close Form" : "+ New Purchase Order"}
                </button>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
                {[
                    { label: "Total Orders", value: orders.length },
                    { label: "Pending Orders", value: pending },
                    { label: "Received Orders", value: received },
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

            {showForm && (
                <form
                    onSubmit={saveOrder}
                    className="space-y-5 rounded-lg border bg-white p-6"
                >
                    <h3 className="text-lg font-semibold">Create Purchase Order</h3>

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Supplier Name
                        </label>
                        <input
                            required
                            value={supplier}
                            onChange={(e) => setSupplier(e.target.value)}
                            placeholder="Enter supplier name"
                            className="w-full rounded-md border border-gray-300 px-4 py-3 text-sm"
                        />
                    </div>

                    <div className="space-y-3">
                        <h4 className="font-medium">Order Items</h4>

                        {items.map((item, index) => (
                            <div
                                key={index}
                                className="grid gap-3 rounded-md border bg-gray-50 p-4 md:grid-cols-12"
                            >
                                <div className="md:col-span-5">
                                    <label className="mb-1 block text-xs">Book Name</label>
                                    <input
                                        required
                                        value={item.book}
                                        onChange={(e) =>
                                            updateItem(index, "book", e.target.value)
                                        }
                                        placeholder="Book title"
                                        className="w-full rounded-md border px-3 py-2"
                                    />
                                </div>

                                <div className="md:col-span-2">
                                    <label className="mb-1 block text-xs">Quantity</label>
                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        required
                                        value={item.quantity}
                                        onChange={(e) =>
                                            updateItem(index, "quantity", e.target.value)
                                        }
                                        className="w-full rounded-md border px-3 py-2"
                                    />
                                </div>

                                <div className="md:col-span-3">
                                    <label className="mb-1 block text-xs">Unit Cost (Rs.)</label>
                                    <input
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        required
                                        value={item.price}
                                        onChange={(e) =>
                                            updateItem(index, "price", e.target.value)
                                        }
                                        className="w-full rounded-md border px-3 py-2"
                                    />
                                </div>

                                <div className="flex items-end md:col-span-2">
                                    <button
                                        type="button"
                                        disabled={items.length === 1}
                                        onClick={() =>
                                            setItems((previous) =>
                                                previous.filter((_, i) => i !== index)
                                            )
                                        }
                                        className="w-full rounded-md border border-red-200 px-3 py-2 text-sm text-red-600 disabled:opacity-40"
                                    >
                                        Remove
                                    </button>
                                </div>
                            </div>
                        ))}

                        <button
                            type="button"
                            onClick={() =>
                                setItems((previous) => [
                                    ...previous,
                                    { book: "", quantity: 1, price: 0 },
                                ])
                            }
                            className="rounded-md border px-4 py-2 text-sm hover:bg-gray-50"
                        >
                            + Add Another Book
                        </button>
                    </div>

                    <div className="border-t pt-4">
                        <p className="text-lg font-bold">
                            Grand Total: {money(calculateTotal(items))}
                        </p>
                    </div>

                    <div className="flex gap-3">
                        <button
                            type="submit"
                            className="rounded-md bg-slate-900 px-5 py-3 text-sm text-white"
                        >
                            Save Purchase Order
                        </button>
                        <button
                            type="button"
                            onClick={resetForm}
                            className="rounded-md border px-5 py-3 text-sm"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            <div className="rounded-lg border bg-white p-6">
                <div className="mb-5">
                    <input
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Search by order ID, supplier or status..."
                        className="w-full rounded-md border px-4 py-3 text-sm"
                    />
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100 text-gray-600">
                        <tr>
                            {["Order ID", "Supplier", "Date", "Items",
                                "Total", "Status", "Action"].map((heading) => (
                                <th key={heading} className="p-4">{heading}</th>
                            ))}
                        </tr>
                        </thead>

                        <tbody>
                        {filtered.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="p-12 text-center text-gray-500">
                                    No purchase orders available.
                                </td>
                            </tr>
                        ) : filtered.map((order) => (
                            <tr key={order.id} className="border-b">
                                <td className="p-4 font-medium">{order.id}</td>
                                <td className="p-4">{order.supplier}</td>
                                <td className="p-4">{order.date}</td>
                                <td className="p-4">{order.items.length}</td>
                                <td className="p-4">
                                    {money(calculateTotal(order.items))}
                                </td>
                                <td className="p-4">
                                    <select
                                        value={order.status}
                                        onChange={(e) =>
                                            changeStatus(
                                                order.id,
                                                e.target.value as PurchaseOrder["status"]
                                            )
                                        }
                                        className="rounded-md border px-2 py-2"
                                    >
                                        <option>Pending</option>
                                        <option>Ordered</option>
                                        <option>Received</option>
                                        <option>Cancelled</option>
                                    </select>
                                </td>
                                <td className="p-4">
                                    <button
                                        onClick={() => setSelected(order)}
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
                    <div className="flex justify-between gap-3">
                        <h3 className="text-lg font-semibold">
                            Order Details — {selected.id}
                        </h3>
                        <button onClick={() => setSelected(null)}>Close</button>
                    </div>
                    <p className="mt-3 text-sm">Supplier: {selected.supplier}</p>
                    <p className="text-sm">Status: {selected.status}</p>
                    <div className="mt-4 space-y-2">
                        {selected.items.map((item, i) => (
                            <div key={i} className="flex justify-between gap-4 border-b py-2 text-sm">
                                <span>{item.book} × {item.quantity}</span>
                                <span>{money(item.price * item.quantity)}</span>
                            </div>
                        ))}
                    </div>
                    <p className="mt-4 font-bold">
                        Total: {money(calculateTotal(selected.items))}
                    </p>
                </div>
            )}
        </div>
    );
}
