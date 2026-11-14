"use client";

import { useEffect, useRef, useState } from "react";
import { apiRequest } from "../lib/api";

type Product = { id: number; sku: string; title: string; price: number; quantity: number };
type CartItem = { productId: number; title: string; price: number; quantity: number };
type SaleReceipt = {
    id: number;
    saleDate: string;
    customerName: string | null;
    totalAmount: number;
    cashierName: string;
    items: { productId: number; sku: string; title: string; quantity: number; unitPrice: number; subtotal: number }[];
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
    const [receiptLookupId, setReceiptLookupId] = useState("");
    const [receiptLoading, setReceiptLoading] = useState(false);
    const [receiptError, setReceiptError] = useState("");
    const [savedSaleId, setSavedSaleId] = useState<number | null>(null);

    async function loadSavedReceipt(saleId: number) {
        setReceiptLoading(true);
        setReceiptError("");
        setReceipt(null);
        try {
            const savedReceipt = await apiRequest<SaleReceipt>(
                `/api/sales/${saleId}/receipt`,
                { cache: "no-store" },
            );
            if (!savedReceipt || savedReceipt.id !== saleId
                || !Number.isFinite(savedReceipt.totalAmount)
                || !Array.isArray(savedReceipt.items)) {
                throw new Error("The server returned an invalid saved receipt.");
            }
            setReceipt(savedReceipt);
            setSavedSaleId(saleId);
        } catch (reason) {
            setReceiptError(reason instanceof Error
                ? reason.message
                : `Unable to retrieve receipt #${saleId}.`);
        } finally {
            setReceiptLoading(false);
        }
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
            const saved = await apiRequest<{ id: number }>("/api/sales", {
                method: "POST",
                body: JSON.stringify({
                    customerName: customer.trim() || "Walk-in Customer",
                    items: cart.map((item) => ({ productId: item.productId, quantity: item.quantity })),
                }),
            });
            if (!saved || !Number.isSafeInteger(saved.id) || saved.id < 1) {
                throw new Error("The server did not confirm a valid sale ID. Check Sales History before retrying.");
            }

            // The sale is committed now. Clear the cart before loading its receipt so a
            // receipt-read failure can never make the user accidentally submit the sale again.
            const committedSaleId = saved.id;
            setCart([]);
            setCustomer("");
            setSelectedBook("");
            setQuantity(1);
            setReceiptLookupId(String(committedSaleId));
            setSavedSaleId(committedSaleId);
            setLoading(true);
            setReload((value) => value + 1);
            await loadSavedReceipt(committedSaleId);
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
                        <button type="button" disabled={saving} onClick={() => { setCart([]); setCustomer(""); setReceipt(null); setReceiptError(""); setError(""); }} className="rounded border px-4 py-2 disabled:opacity-50">Clear Cart</button>
                        <button type="button" disabled={saving || cart.length === 0} onClick={() => void confirmSale()} className="rounded bg-blue-600 px-5 py-2 text-white disabled:opacity-50">{saving ? "Saving sale..." : "Confirm Cash Sale"}</button>
                    </div>
                </div>
            </section>
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-lg font-semibold">Retrieve a saved receipt</h3>
                <form className="mt-3 flex flex-wrap gap-3" onSubmit={(event) => {
                    event.preventDefault();
                    const id = Number(receiptLookupId);
                    if (Number.isSafeInteger(id) && id > 0) void loadSavedReceipt(id);
                    else setReceiptError("Enter a valid saved sale ID.");
                }}>
                    <label htmlFor="receipt-sale-id" className="sr-only">Saved sale ID</label>
                    <input id="receipt-sale-id" inputMode="numeric" pattern="[0-9]*"
                        value={receiptLookupId} onChange={(event) => setReceiptLookupId(event.target.value)}
                        placeholder="Enter sale ID" className="min-w-0 flex-1 rounded border border-gray-300 px-3 py-2" />
                    <button type="submit" disabled={receiptLoading || !receiptLookupId.trim()}
                        className="rounded bg-slate-800 px-4 py-2 text-white disabled:opacity-50">
                        {receiptLoading ? "Loading receipt..." : "Load receipt"}
                    </button>
                </form>
                {receiptError && <p role="alert" className="mt-3 text-sm text-red-700">{receiptError}</p>}
                {savedSaleId !== null && !receipt && !receiptError && (
                    <p role="status" className="mt-3 text-sm text-green-800">Sale #{savedSaleId} was saved. Its receipt is not currently loaded.</p>
                )}
            </section>
            {receipt && <section className="rounded border border-green-200 bg-white p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                        <h3 className="text-xl font-bold text-green-800">Saved receipt #{receipt.id}</h3>
                        <p className="mt-1 text-sm text-gray-600">{new Date(receipt.saleDate).toLocaleString("en-PK")}</p>
                    </div>
                    <p className="text-lg font-bold">{money(receipt.totalAmount)}</p>
                </div>
                <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
                    <p>Customer: {receipt.customerName || "Walk-in Customer"}</p>
                    <p>Cashier: {receipt.cashierName}</p>
                </div>
                <div className="mt-4 overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100"><tr><th className="p-3">Book</th><th className="p-3">SKU</th><th className="p-3">Qty</th><th className="p-3">Unit price</th><th className="p-3">Subtotal</th></tr></thead>
                        <tbody>{receipt.items.map((item) => (
                            <tr key={item.productId} className="border-t">
                                <td className="p-3">{item.title}</td><td className="p-3 font-mono text-xs">{item.sku}</td>
                                <td className="p-3">{item.quantity}</td><td className="p-3">{money(item.unitPrice)}</td><td className="p-3">{money(item.subtotal)}</td>
                            </tr>
                        ))}</tbody>
                    </table>
                </div>
            </section>}
        </div>
    );
}
