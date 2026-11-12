"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";

type OrderStatus = "PENDING" | "READY" | "DELIVERED";
type School = { id: number; name: string };
type Product = { id: number; title: string; sku: string };
type OrderItem = { productId: number; title: string; quantity: number; unitPrice: number };
type SchoolOrder = {
    id: number;
    schoolId: number;
    schoolName: string;
    orderDate: string;
    status: OrderStatus;
    items: OrderItem[];
};
type Quotation = {
    orderId: number;
    schoolName: string;
    orderDate: string;
    items: Array<{ title: string; quantity: number; unitPrice: number; lineTotal: number }>;
    totalAmount: number;
};

const statuses: OrderStatus[] = ["PENDING", "READY", "DELIVERED"];

export default function SchoolOrders() {
    const [orders, setOrders] = useState<SchoolOrder[]>([]);
    const [schools, setSchools] = useState<School[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [schoolId, setSchoolId] = useState("");
    const [productId, setProductId] = useState("");
    const [quantity, setQuantity] = useState("1");
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [busyOrderId, setBusyOrderId] = useState<number | null>(null);
    const [error, setError] = useState("");
    const [formError, setFormError] = useState("");
    const [notice, setNotice] = useState("");
    const [quotation, setQuotation] = useState<Quotation | null>(null);

    const loadData = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const [savedOrders, registeredSchools, availableProducts] = await Promise.all([
                apiRequest<SchoolOrder[]>("/api/school-orders"),
                apiRequest<School[]>("/api/schools"),
                apiRequest<Product[]>("/api/products"),
            ]);
            setOrders(savedOrders);
            setSchools(registeredSchools);
            setProducts(availableProducts);
        } catch {
            setError("Unable to load school orders. Check your connection and Admin permissions, then retry.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let isMounted = true;

        Promise.all([
            apiRequest<SchoolOrder[]>("/api/school-orders"),
            apiRequest<School[]>("/api/schools"),
            apiRequest<Product[]>("/api/products"),
        ])
            .then(([savedOrders, registeredSchools, availableProducts]) => {
                if (!isMounted) return;
                setOrders(savedOrders);
                setSchools(registeredSchools);
                setProducts(availableProducts);
            })
            .catch(() => {
                if (isMounted) {
                    setError("Unable to load school orders. Check your connection and Admin permissions, then retry.");
                }
            })
            .finally(() => {
                if (isMounted) setLoading(false);
            });

        return () => {
            isMounted = false;
        };
    }, []);

    async function createOrder(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (saving) return;
        const selectedSchool = Number(schoolId);
        const selectedProduct = Number(productId);
        const requestedQuantity = Number(quantity);
        if (!selectedSchool || !selectedProduct || !Number.isInteger(requestedQuantity) || requestedQuantity < 1) {
            setFormError("Select a school and product, and enter a positive whole-number quantity.");
            return;
        }
        setSaving(true);
        setFormError("");
        setNotice("");
        try {
            await apiRequest<SchoolOrder>("/api/school-orders", {
                method: "POST",
                body: JSON.stringify({
                    schoolId: selectedSchool,
                    items: [{ productId: selectedProduct, quantity: requestedQuantity }],
                }),
            });
            setShowForm(false);
            setSchoolId("");
            setProductId("");
            setQuantity("1");
            setNotice("School order saved successfully.");
            await loadData();
        } catch {
            setFormError("Could not save the order. Check stock, selection and your connection, then retry.");
        } finally {
            setSaving(false);
        }
    }

    async function changeStatus(order: SchoolOrder, status: OrderStatus) {
        if (busyOrderId !== null || order.status === status) return;
        if (!window.confirm(`Change order #${order.id} status to ${status}?`)) return;
        setBusyOrderId(order.id);
        setError("");
        setNotice("");
        try {
            await apiRequest<SchoolOrder>(`/api/school-orders/${order.id}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status }),
            });
            setNotice(`Order #${order.id} updated to ${status}.`);
            await loadData();
        } catch {
            setError("Status update failed. Please retry after checking your connection and permissions.");
        } finally {
            setBusyOrderId(null);
        }
    }

    async function viewQuotation(orderId: number) {
        if (busyOrderId !== null) return;
        setBusyOrderId(orderId);
        setError("");
        setQuotation(null);
        try {
            setQuotation(await apiRequest<Quotation>(`/api/school-orders/${orderId}/quotation`));
        } catch {
            setError("Could not retrieve the quotation. Please try again.");
        } finally {
            setBusyOrderId(null);
        }
    }

    const filteredOrders = orders.filter((order) =>
        order.schoolName.toLowerCase().includes(search.toLowerCase()) ||
        order.items.some((item) => item.title.toLowerCase().includes(search.toLowerCase()))
    );
    const pendingOrders = orders.filter((order) => order.status === "PENDING").length;
    const totalAmount = orders.reduce((sum, order) =>
        sum + order.items.reduce((lineSum, item) => lineSum + item.quantity * item.unitPrice, 0), 0
    );

    return (
        <div className="space-y-7">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <p className="text-sm text-gray-600">Manage saved school orders. Editing and deleting orders are not supported by the current API.</p>
                <button type="button" onClick={() => { setShowForm((value) => !value); setFormError(""); }}
                    className="rounded bg-blue-600 px-5 py-3 text-white hover:bg-blue-700">
                    {showForm ? "Cancel" : "+ Add School Order"}
                </button>
            </div>

            {notice && <p role="status" className="rounded bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
            {error && <div role="alert" className="rounded border border-red-200 bg-white p-4 text-red-700">
                <p>{error}</p>
                <button type="button" onClick={() => void loadData()} className="mt-2 underline">Retry loading</button>
            </div>}

            <div className="grid gap-5 sm:grid-cols-3">
                <div className="rounded border bg-white p-6"><p>Total Orders</p><p className="mt-3 text-2xl font-bold">{orders.length}</p></div>
                <div className="rounded border bg-white p-6"><p>Pending Orders</p><p className="mt-3 text-2xl font-bold">{pendingOrders}</p></div>
                <div className="rounded border bg-white p-6"><p>Total Amount</p><p className="mt-3 text-2xl font-bold">Rs. {totalAmount.toLocaleString()}</p></div>
            </div>

            {showForm && (
                <form onSubmit={(event) => void createOrder(event)} aria-busy={saving} className="space-y-4 rounded border bg-white p-6">
                    <h3 className="text-xl font-semibold">Add School Order</h3>
                    {formError && <p role="alert" id="school-order-form-error" className="text-red-700">{formError}</p>}
                    <div className="grid gap-4 sm:grid-cols-3">
                        <div><label htmlFor="school-order-school" className="mb-2 block">School</label>
                            <select id="school-order-school" required disabled={saving} value={schoolId}
                                onChange={(event) => setSchoolId(event.target.value)} className="w-full rounded border p-3">
                                <option value="">Select school</option>
                                {schools.map((school) => <option key={school.id} value={school.id}>{school.name}</option>)}
                            </select></div>
                        <div><label htmlFor="school-order-product" className="mb-2 block">Book</label>
                            <select id="school-order-product" required disabled={saving} value={productId}
                                onChange={(event) => setProductId(event.target.value)} className="w-full rounded border p-3">
                                <option value="">Select book</option>
                                {products.map((product) => <option key={product.id} value={product.id}>{product.title} ({product.sku})</option>)}
                            </select></div>
                        <div><label htmlFor="school-order-quantity" className="mb-2 block">Quantity</label>
                            <input id="school-order-quantity" type="number" min="1" step="1" required disabled={saving}
                                value={quantity} onChange={(event) => setQuantity(event.target.value)}
                                className="w-full rounded border p-3" /></div>
                    </div>
                    <button type="submit" disabled={saving || loading || schools.length === 0 || products.length === 0}
                        className="rounded bg-blue-600 px-5 py-3 text-white disabled:opacity-50">
                        {saving ? "Saving..." : "Save Order"}
                    </button>
                    {(!schools.length || !products.length) && !loading && <p className="text-sm text-gray-600">
                        Register a school and add inventory products before creating an order.
                    </p>}
                </form>
            )}

            <section className="rounded border bg-white p-6">
                <div className="mb-5 flex flex-wrap items-center justify-between gap-4">
                    <h3 className="text-xl font-semibold">School Orders List</h3>
                    <label htmlFor="school-order-search" className="sr-only">Search school orders by school or book</label>
                    <input id="school-order-search" type="search" placeholder="Search school or book..." value={search}
                        onChange={(event) => setSearch(event.target.value)} className="w-full rounded border px-4 py-3 sm:w-72" />
                </div>
                {loading ? <p role="status">Loading school orders...</p> : !error && (
                    <div className="overflow-x-auto"><table className="w-full text-left text-sm">
                        <thead className="bg-gray-100"><tr>
                            <th scope="col" className="p-4">School</th>
                            <th scope="col" className="p-4">Books</th>
                            <th scope="col" className="p-4">Total (Rs.)</th>
                            <th scope="col" className="p-4">Status</th>
                            <th scope="col" className="p-4">Actions</th>
                        </tr></thead>
                        <tbody>
                            {filteredOrders.map((order) => (
                                <tr key={order.id} className="border-b">
                                    <td className="p-4">{order.schoolName}</td>
                                    <td className="p-4">{order.items.map((item) => `${item.title} × ${item.quantity}`).join(", ")}</td>
                                    <td className="p-4">{order.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0).toLocaleString()}</td>
                                    <td className="p-4">
                                        <label htmlFor={`order-status-${order.id}`} className="sr-only">Status for order #{order.id}</label>
                                        <select id={`order-status-${order.id}`} value={order.status}
                                            disabled={busyOrderId !== null}
                                            onChange={(event) => void changeStatus(order, event.target.value as OrderStatus)}
                                            className="rounded border p-2">
                                            {statuses.map((status) => <option key={status} value={status}>{status}</option>)}
                                        </select>
                                    </td>
                                    <td className="p-4">
                                        <button type="button" disabled={busyOrderId !== null}
                                            onClick={() => void viewQuotation(order.id)}
                                            className="text-blue-700 underline disabled:opacity-50">View quotation</button>
                                    </td>
                                </tr>
                            ))}
                            {filteredOrders.length === 0 && <tr><td colSpan={5} className="p-8 text-center text-gray-500">
                                {search ? "No matching orders found." : "No school orders found."}
                            </td></tr>}
                        </tbody>
                    </table></div>
                )}
            </section>

            {quotation && (
                <section aria-label="School order quotation" className="rounded border bg-white p-6">
                    <div className="flex flex-wrap justify-between gap-3">
                        <h3 className="text-xl font-semibold">Quotation #{quotation.orderId}</h3>
                        <button type="button" onClick={() => setQuotation(null)} className="underline">Close quotation</button>
                    </div>
                    <p className="mt-2">{quotation.schoolName} — {quotation.orderDate}</p>
                    <ul className="mt-4 space-y-2">
                        {quotation.items.map((item, index) =>
                            <li key={index}>{item.title}: {item.quantity} × Rs. {item.unitPrice} = Rs. {item.lineTotal}</li>)}
                    </ul>
                    <p className="mt-4 font-bold">Grand Total: Rs. {quotation.totalAmount.toLocaleString()}</p>
                </section>
            )}
        </div>
    );
}
