"use client";

import { useEffect, useRef, useState } from "react";
import { apiRequest } from "../lib/api";

type Product = { id: number; sku: string; title: string; price: number; quantity: number };
type CartItem = { productId: number; title: string; price: number; quantity: number };
type SaleReceipt = {
    id: number;
    saleDate: string;
    customerName: string;
    totalAmount: number;
    cashierName: string;
    items: { productId: number; title: string; quantity: number; unitPrice: number; subtotal: number }[];
};
const money = (amount: number) => `Rs. ${amount.toLocaleString("en-PK", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function SalesPOS() {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [reload, setReload] = useState(0);
    const [selectedBook, setSelectedBook] = useState("");
    const [quantity, setQuantity] = useState(1);
    const [cart, setCart] = useState<CartItem[]>([]);
    const [customer, setCustomer] = useState("");
    const [saving, setSaving] = useState(false);
    const pending = useRef(false);
    const [error, setError] = useState("");
    const [receipt, setReceipt] = useState<SaleReceipt | null>(null);
    const [receiptId, setReceiptId] = useState("");
    const [receiptLoading, setReceiptLoading] = useState(false);
    const [receiptError, setReceiptError] = useState("");
    const receiptPending = useRef(false);

    async function loadReceipt(id: number) {
        if (receiptPending.current) return;
        receiptPending.current = true;
        setReceiptLoading(true);
        setReceiptError("");
        setReceipt(null);
        try {
            const saved = await apiRequest<SaleReceipt>(`/api/sales/${id}/receipt`, { cache: "no-store" });
            if (!saved || saved.id !== id || !Number.isFinite(saved.totalAmount)
                || !saved.saleDate || !Array.isArray(saved.items)
                || saved.items.some((item) => !item.title || !Number.isFinite(item.unitPrice)
                    || !Number.isFinite(item.subtotal) || !Number.isSafeInteger(item.quantity) || item.quantity < 1)) {
                throw new Error("The saved receipt response is incomplete. Please try again.");
            }
            setReceipt(saved);
            setReceiptId(String(id));
        } catch (reason) {
            setReceiptError(reason instanceof Error ? reason.message : "Unable to load saved receipt.");
        } finally {
            receiptPending.current = false;
            setReceiptLoading(false);
        }
    }

    function findReceipt() {
        const id = Number(receiptId);
        if (!Number.isSafeInteger(id) || id < 1) {
            setReceiptError("Enter a valid positive sale ID.");
            setReceipt(null);
            return;
        }
        void loadReceipt(id);
    }

    useEffect(() => {
        let active = true;
        apiRequest<Product[]>("/api/products", { cache: "no-store" })
            .then((data) => {
                if (!Array.isArray(data)) throw new Error("Invalid inventory response.");
                if (active) { setProducts(data); setLoadError(""); }
            })
            .catch(() => { if (active) setLoadError("Unable to load inventory. Check your connection and permissions."); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [reload]);

    function addToCart() {
        setError("");
        const book = products.find((item) => item.id === Number(selectedBook));
        if (!book) { setError("Select a book from inventory."); return; }
        if (!Number.isSafeInteger(quantity) || quantity < 1) {
            setError("Quantity must be a positive whole number."); return;
        }
        const existing = cart.find((item) => item.productId === book.id);
        const nextQuantity = (existing?.quantity ?? 0) + quantity;
        if (!Number.isSafeInteger(nextQuantity) || nextQuantity > book.quantity) {
            setError(`Only ${book.quantity} copies of ${book.title} are currently available.`); return;
        }
        setCart((previous) => existing
            ? previous.map((item) => item.productId === book.id ? { ...item, quantity: nextQuantity } : item)
            : [...previous, { productId: book.id, title: book.title, price: book.price, quantity }]);
        setSelectedBook("");
        setQuantity(1);
        setReceipt(null);
    }

    async function confirmSale() {
        if (pending.current) return;
        if (cart.length === 0) { setError("Add at least one book before checkout."); return; }
        if (!window.confirm("Confirm this cash sale? Inventory will be deducted.")) return;
        pending.current = true;
        setSaving(true);
        setError("");
        setReceipt(null);
        try {
            const saved = await apiRequest<SaleReceipt>("/api/sales", {
                method: "POST",
                body: JSON.stringify({
                    customerName: customer.trim() || "Walk-in Customer",
                    items: cart.map((item) => ({ productId: item.productId, quantity: item.quantity })),
                }),
            });
            if (!saved || !Number.isFinite(saved.totalAmount) || !Array.isArray(saved.items)) {
                throw new Error("Sale response could not be verified. Check saved receipts before retrying.");
            }
            setCart([]);
            setReceiptId(String(saved.id));
            void loadReceipt(saved.id);
            setCustomer("");
            setSelectedBook("");
            setQuantity(1);
            setLoading(true);
            setReload((value) => value + 1);
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : "Checkout failed. Your cart was retained.");
        } finally {
            pending.current = false;
            setSaving(false);
        }
    }

    const estimatedTotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const inputStyle = "w-full rounded border border-gray-300 p-3 disabled:opacity-50";

    return (
        <div className="space-y-6">
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-5 text-xl font-semibold">New Cash Sale</h3>
                {error && <p role="alert" className="mb-4 rounded bg-red-50 p-3 text-red-700">{error}</p>}
                {loadError && <div role="alert" className="mb-4"><p>{loadError}</p><button type="button" className="underline" onClick={() => { setLoading(true); setReload((value) => value + 1); }}>Retry inventory</button></div>}
                {loading && <p role="status" className="mb-4">Loading inventory...</p>}
                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                    <div>
                        <label htmlFor="sales-customer-name" className="mb-2 block text-sm">Customer Name</label>
                        <input id="sales-customer-name" className={inputStyle} disabled={saving} value={customer} onChange={(event) => setCustomer(event.target.value)} placeholder="Walk-in Customer" />
                    </div>
                    <div>
                        <label htmlFor="sales-book" className="mb-2 block text-sm">Select Book</label>
                        <select id="sales-book" className={inputStyle} disabled={saving || loading || Boolean(loadError)} value={selectedBook} onChange={(event) => setSelectedBook(event.target.value)}>
                            <option value="">Choose a book</option>
                            {products.filter((product) => product.quantity > 0).map((book) => (
                                <option key={book.id} value={book.id}>{book.title} ({book.sku}) — {money(book.price)} — {book.quantity} in stock</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="sales-quantity" className="mb-2 block text-sm">Quantity</label>
                        <input id="sales-quantity" type="number" min="1" step="1" className={inputStyle} disabled={saving} value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} />
                    </div>
                </div>
                <button type="button" disabled={saving || loading || Boolean(loadError)} onClick={addToCart} className="mt-5 rounded bg-blue-600 px-5 py-3 text-white disabled:opacity-50">+ Add to Cart</button>
            </section>
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-5 text-xl font-semibold">Shopping Cart</h3>
                <p className="mb-3 text-sm text-gray-600">Prices below are estimates. The backend confirms the final cash total.</p>
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100"><tr><th className="p-4">Book</th><th className="p-4">Price</th><th className="p-4">Qty</th><th className="p-4">Subtotal</th><th className="p-4">Action</th></tr></thead>
                        <tbody>
                            {cart.length === 0 ? <tr><td colSpan={5} className="p-8 text-center text-gray-500">No books added to cart.</td></tr> : cart.map((item) => (
                                <tr key={item.productId} className="border-t">
                                    <td className="p-4">{item.title}</td><td className="p-4">{money(item.price)}</td><td className="p-4">{item.quantity}</td><td className="p-4">{money(item.price * item.quantity)}</td>
                                    <td className="p-4"><button type="button" disabled={saving} aria-label={`Remove ${item.title} from cart`} onClick={() => setCart((previous) => previous.filter((entry) => entry.productId !== item.productId))} className="text-red-700 disabled:opacity-50">Remove</button></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
                    <h4 className="text-xl font-bold">Estimated total: {money(estimatedTotal)}</h4>
                    <div className="flex gap-3">
                        <button type="button" disabled={saving} onClick={() => { setCart([]); setCustomer(""); setReceipt(null); setError(""); }} className="rounded border px-4 py-2 disabled:opacity-50">Clear Cart</button>
                        <button type="button" disabled={saving || cart.length === 0} onClick={() => void confirmSale()} className="rounded bg-blue-600 px-5 py-2 text-white disabled:opacity-50">{saving ? "Saving sale..." : "Confirm Cash Sale"}</button>
                    </div>
                </div>
            </section>
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-4 text-xl font-semibold">Saved Sale Receipt</h3>
                <div className="flex flex-wrap items-end gap-3">
                    <div>
                        <label htmlFor="pos-receipt-id" className="mb-2 block text-sm">Sale ID</label>
                        <input id="pos-receipt-id" type="number" min="1" step="1" value={receiptId}
                            onChange={(event) => setReceiptId(event.target.value)}
                            className="rounded border border-gray-300 p-3" />
                    </div>
                    <button type="button" disabled={receiptLoading} onClick={findReceipt}
                        className="rounded bg-blue-600 px-5 py-3 text-white disabled:opacity-50">
                        {receiptLoading ? "Loading receipt..." : "Find Receipt"}
                    </button>
                </div>
                {receiptError && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-700">{receiptError}</p>}
                {receiptLoading && <p role="status" className="mt-4">Loading saved receipt...</p>}
                {receipt && <div className="mt-6 space-y-3" aria-label={`Saved receipt for sale ${receipt.id}`}>
                    <h4 className="text-lg font-bold">Qadri Books — Receipt #{receipt.id}</h4>
                    <p><strong>Sale date:</strong> {receipt.saleDate}</p>
                    <p><strong>Customer:</strong> {receipt.customerName || "Walk-in Customer"}</p>
                    <p><strong>Cashier:</strong> {receipt.cashierName || "Not recorded"}</p>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-100"><tr>
                                <th scope="col" className="p-3">Book</th>
                                <th scope="col" className="p-3">Qty</th>
                                <th scope="col" className="p-3">Saved unit price</th>
                                <th scope="col" className="p-3">Subtotal</th>
                            </tr></thead>
                            <tbody>{receipt.items.map((item, index) => <tr key={`${item.productId}-${index}`} className="border-b">
                                <td className="p-3">{item.title}</td>
                                <td className="p-3">{item.quantity}</td>
                                <td className="p-3">{money(item.unitPrice)}</td>
                                <td className="p-3">{money(item.subtotal)}</td>
                            </tr>)}</tbody>
                        </table>
                    </div>
                    <p className="text-right text-xl font-bold">Saved total: {money(receipt.totalAmount)}</p>
                </div>}
            </section>
        </div>
    );
}
