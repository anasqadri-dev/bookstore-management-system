"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";

type Supplier = { id: number; name: string };
type Product = { id: number; title: string; sku: string };
type PurchaseItem = { productId: number; title: string; quantity: number; unitCost: number };
type PurchaseOrder = {
    id: number;
    supplierId: number;
    supplierName: string;
    orderDate: string;
    status: "ORDERED" | "RECEIVED";
    receivedDate: string | null;
    items: PurchaseItem[];
};
type DraftItem = { productId: string; quantity: string; unitCost: string };
const blankItem = (): DraftItem => ({ productId: "", quantity: "1", unitCost: "" });
const money = (value: number) => `Rs. ${value.toLocaleString()}`;
const total = (items: PurchaseItem[]) => items.reduce((sum, item) => sum + item.quantity * item.unitCost, 0);

export default function PurchaseOrders() {
    const [orders, setOrders] = useState<PurchaseOrder[]>([]);
    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [supplierId, setSupplierId] = useState("");
    const [items, setItems] = useState<DraftItem[]>([blankItem()]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [receivingId, setReceivingId] = useState<number | null>(null);
    const [error, setError] = useState("");
    const [formError, setFormError] = useState("");
    const [notice, setNotice] = useState("");

    const loadData = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const [saved, availableSuppliers, availableProducts] = await Promise.all([
                apiRequest<PurchaseOrder[]>("/api/purchase-orders"),
                apiRequest<Supplier[]>("/api/suppliers"),
                apiRequest<Product[]>("/api/products"),
            ]);
            setOrders(saved);
            setSuppliers(availableSuppliers);
            setProducts(availableProducts);
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : "Unable to load purchase orders.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function loadInitialData() {
            try {
                const [saved, availableSuppliers, availableProducts] = await Promise.all([
                    apiRequest<PurchaseOrder[]>("/api/purchase-orders"),
                    apiRequest<Supplier[]>("/api/suppliers"),
                    apiRequest<Product[]>("/api/products"),
                ]);
                if (!cancelled) {
                    setOrders(saved);
                    setSuppliers(availableSuppliers);
                    setProducts(availableProducts);
                }
            } catch (cause) {
                if (!cancelled) {
                    setError(cause instanceof Error ? cause.message : "Unable to load purchase orders.");
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        void loadInitialData();
        return () => { cancelled = true; };
    }, []);

    function resetForm() {
        setSupplierId("");
        setItems([blankItem()]);
        setFormError("");
        setShowForm(false);
    }

    function updateItem(index: number, field: keyof DraftItem, value: string) {
        setItems((previous) => previous.map((item, i) => i === index ? { ...item, [field]: value } : item));
    }

    async function createOrder(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (saving) return;
        const parsed = items.map((item) => ({
            productId: Number(item.productId),
            quantity: Number(item.quantity),
            unitCost: Number(item.unitCost),
        }));
        if (!Number(supplierId) || parsed.length === 0 || parsed.some((item) =>
            !Number.isInteger(item.productId) || item.productId < 1 ||
            !Number.isInteger(item.quantity) || item.quantity < 1 ||
            !Number.isFinite(item.unitCost) || item.unitCost <= 0
        )) {
            setFormError("Select a supplier and books, and enter positive quantities and unit costs.");
            return;
        }
        setSaving(true);
        setFormError("");
        setNotice("");
        try {
            await apiRequest<PurchaseOrder>("/api/purchase-orders", {
                method: "POST",
                body: JSON.stringify({ supplierId: Number(supplierId), items: parsed }),
            });
            resetForm();
            setNotice("Purchase order created with ORDERED status.");
            await loadData();
        } catch (cause) {
            setFormError(cause instanceof Error ? cause.message : "Purchase order could not be saved.");
        } finally {
            setSaving(false);
        }
    }

    async function receiveOrder(order: PurchaseOrder) {
        if (order.status !== "ORDERED" || receivingId !== null) return;
        if (!window.confirm(`Receive purchase order #${order.id}? This will add its quantities to inventory.`)) return;
        setReceivingId(order.id);
        setError("");
        setNotice("");
        try {
            await apiRequest<PurchaseOrder>(`/api/purchase-orders/${order.id}/receive`, { method: "POST" });
            // Display RECEIVED only when the server has confirmed the transaction.
            setOrders((previous) => previous.map((saved) =>
                saved.id === order.id ? { ...saved, status: "RECEIVED" } : saved
            ));
            setNotice(`Order #${order.id} received; stock has been updated.`);
            await loadData();
        } catch (cause) {
            // A 409 conflict (including duplicate receipt) must not change the displayed status.
            setError(cause instanceof Error ? cause.message : "Goods receipt failed. Please retry.");
        } finally {
            setReceivingId(null);
        }
    }

    const filtered = orders.filter((order) =>
        `${order.id} ${order.supplierName} ${order.status}`.toLowerCase().includes(search.toLowerCase())
    );
    const selected = orders.find((order) => order.id === selectedId);
    const orderedCount = orders.filter((order) => order.status === "ORDERED").length;
    const receivedCount = orders.filter((order) => order.status === "RECEIVED").length;

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h3 className="text-xl font-semibold">Purchase Management</h3>
                    <p className="mt-1 text-sm text-gray-600">Create purchase orders and record goods receipt through the backend.</p>
                </div>
                <button type="button" onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}
                    className="rounded bg-slate-900 px-5 py-3 text-sm text-white">
                    {showForm ? "Close Form" : "+ New Purchase Order"}
                </button>
            </div>

            {notice && <p role="status" className="rounded bg-green-50 p-3 text-green-800">{notice}</p>}
            {error && <div role="alert" className="rounded border border-red-200 p-4 text-red-700">
                <p>{error}</p>
                <button type="button" onClick={() => void loadData()} className="mt-2 underline">Reload orders</button>
            </div>}

            <div className="grid gap-4 sm:grid-cols-3">
                {[
                    { label: "Total Orders", value: orders.length },
                    { label: "Ordered", value: orderedCount },
                    { label: "Received", value: receivedCount },
                ].map((card) => <div key={card.label} className="rounded border bg-white p-6">
                    <p className="text-sm text-gray-600">{card.label}</p>
                    <p className="mt-3 text-2xl font-bold">{card.value}</p>
                </div>)}
            </div>

            {showForm && <form onSubmit={(event) => void createOrder(event)}
                className="space-y-5 rounded border bg-white p-6" aria-busy={saving}>
                <h3 className="text-lg font-semibold">Create Purchase Order</h3>
                {formError && <p role="alert" className="text-red-700">{formError}</p>}
                <div>
                    <label htmlFor="purchase-supplier" className="mb-2 block text-sm font-medium">Supplier</label>
                    <select id="purchase-supplier" required disabled={saving} value={supplierId}
                        onChange={(event) => setSupplierId(event.target.value)} className="w-full rounded border p-3">
                        <option value="">Select supplier</option>
                        {suppliers.map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
                    </select>
                </div>
                <div className="space-y-3">
                    <h4 className="font-medium">Order Items</h4>
                    {items.map((item, index) => <div key={index} className="grid gap-3 rounded border bg-gray-50 p-4 md:grid-cols-12">
                        <div className="md:col-span-5">
                            <label htmlFor={`purchase-product-${index}`} className="mb-1 block text-sm">Book</label>
                            <select id={`purchase-product-${index}`} required disabled={saving} value={item.productId}
                                onChange={(event) => updateItem(index, "productId", event.target.value)}
                                className="w-full rounded border p-2">
                                <option value="">Select book</option>
                                {products.map((product) => <option key={product.id} value={product.id}>{product.title} ({product.sku})</option>)}
                            </select>
                        </div>
                        <div className="md:col-span-2">
                            <label htmlFor={`purchase-quantity-${index}`} className="mb-1 block text-sm">Quantity</label>
                            <input id={`purchase-quantity-${index}`} type="number" min="1" step="1" required
                                disabled={saving} value={item.quantity}
                                onChange={(event) => updateItem(index, "quantity", event.target.value)}
                                className="w-full rounded border p-2" />
                        </div>
                        <div className="md:col-span-3">
                            <label htmlFor={`purchase-cost-${index}`} className="mb-1 block text-sm">Unit Cost (Rs.)</label>
                            <input id={`purchase-cost-${index}`} type="number" min="0.01" step="0.01" required
                                disabled={saving} value={item.unitCost}
                                onChange={(event) => updateItem(index, "unitCost", event.target.value)}
                                className="w-full rounded border p-2" />
                        </div>
                        <div className="flex items-end md:col-span-2">
                            <button type="button" disabled={saving || items.length === 1}
                                onClick={() => setItems((previous) => previous.filter((_, i) => i !== index))}
                                className="w-full rounded border p-2 text-red-700 disabled:opacity-40">Remove</button>
                        </div>
                    </div>)}
                    <button type="button" disabled={saving} onClick={() => setItems((previous) => [...previous, blankItem()])}
                        className="rounded border px-4 py-2">+ Add Another Book</button>
                </div>
                <div className="flex flex-wrap gap-3">
                    <button type="submit" disabled={saving || loading || !suppliers.length || !products.length}
                        className="rounded bg-slate-900 px-5 py-3 text-white disabled:opacity-50">
                        {saving ? "Saving..." : "Save Purchase Order"}
                    </button>
                    <button type="button" disabled={saving} onClick={resetForm} className="rounded border px-5 py-3">Cancel</button>
                </div>
                {!loading && (!suppliers.length || !products.length) && <p className="text-sm text-gray-600">
                    Register suppliers and products before creating a purchase order.
                </p>}
            </form>}

            <section className="rounded border bg-white p-6">
                <label htmlFor="purchase-search" className="sr-only">Search purchase orders</label>
                <input id="purchase-search" type="search" placeholder="Search by order ID, supplier or status..."
                    value={search} onChange={(event) => setSearch(event.target.value)}
                    className="mb-5 w-full rounded border px-4 py-3 sm:w-80" />
                {loading ? <p role="status">Loading purchase orders...</p> : !error && (
                    <div className="overflow-x-auto"><table className="w-full text-left text-sm">
                        <thead className="bg-gray-100"><tr>
                            {["Order ID", "Supplier", "Date", "Items", "Total", "Status", "Action"].map((heading) =>
                                <th key={heading} scope="col" className="p-4">{heading}</th>)}
                        </tr></thead>
                        <tbody>
                            {filtered.map((order) => <tr key={order.id} className="border-b">
                                <td className="p-4">{order.id}</td>
                                <td className="p-4">{order.supplierName}</td>
                                <td className="p-4">{order.orderDate}</td>
                                <td className="p-4">{order.items.length}</td>
                                <td className="p-4">{money(total(order.items))}</td>
                                <td className="p-4">{order.status}</td>
                                <td className="p-4"><div className="flex gap-3">
                                    <button type="button" onClick={() => setSelectedId(order.id)}
                                        className="text-blue-700 underline">View</button>
                                    {order.status === "ORDERED" && <button type="button"
                                        disabled={receivingId !== null} onClick={() => void receiveOrder(order)}
                                        className="text-green-800 underline disabled:opacity-50">
                                        {receivingId === order.id ? "Receiving..." : "Receive goods"}
                                    </button>}
                                </div></td>
                            </tr>)}
                            {filtered.length === 0 && <tr><td colSpan={7} className="p-8 text-center text-gray-500">
                                {search ? "No matching purchase orders." : "No purchase orders available."}
                            </td></tr>}
                        </tbody>
                    </table></div>
                )}
            </section>

            {selected && <section className="rounded border bg-white p-6">
                <div className="flex justify-between gap-3">
                    <h3 className="text-lg font-semibold">Order Details — #{selected.id}</h3>
                    <button type="button" onClick={() => setSelectedId(null)} className="underline">Close</button>
                </div>
                <p className="mt-3">Supplier: {selected.supplierName}</p>
                <p>Status: {selected.status}</p>
                {selected.receivedDate && <p>Received on: {selected.receivedDate}</p>}
                <ul className="mt-4 space-y-2">
                    {selected.items.map((item, index) => <li key={index} className="border-b py-2">
                        {item.title} × {item.quantity} — {money(item.quantity * item.unitCost)}
                    </li>)}
                </ul>
                <p className="mt-4 font-bold">Total: {money(total(selected.items))}</p>
            </section>}
        </div>
    );
}
