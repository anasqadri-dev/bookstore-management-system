
"use client";

import { useState } from "react";

type SaleItem = {
    id: number;
    title: string;
    price: number;
    quantity: number;
};

const availableBooks = [
    { id: 1, title: "Java Programming", price: 1500 },
    { id: 2, title: "Python Basics", price: 1200 },
    { id: 3, title: "Data Structures", price: 1800 },
    { id: 4, title: "Database Systems", price: 2000 },
];

export default function SalesPOS() {
    const [selectedBook, setSelectedBook] = useState("");
    const [quantity, setQuantity] = useState(1);
    const [cart, setCart] = useState<SaleItem[]>([]);
    const [customer, setCustomer] = useState("");
    const [showBill, setShowBill] = useState(false);

    function addToCart() {
        const book = availableBooks.find(
            (item) => item.id === Number(selectedBook)
        );

        if (!book) {
            alert("Please select a book.");
            return;
        }

        if (!Number.isSafeInteger(quantity) || quantity < 1) {
            alert("Please enter a valid quantity.");
            return;
        }

        setCart((previous) => {
            const existing = previous.find((item) => item.id === book.id);

            if (existing) {
                return previous.map((item) =>
                    item.id === book.id
                        ? { ...item, quantity: item.quantity + quantity }
                        : item
                );
            }

            return [...previous, { ...book, quantity }];
        });

        setSelectedBook("");
        setQuantity(1);
        setShowBill(false);
    }

    function removeFromCart(id: number) {
        setCart((previous) => previous.filter((item) => item.id !== id));
        setShowBill(false);
    }

    const total = cart.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );

    function generateBill() {
        if (cart.length === 0) {
            alert("Please add books to the cart first.");
            return;
        }

        setShowBill(true);
    }

    function newSale() {
        setCart([]);
        setCustomer("");
        setSelectedBook("");
        setQuantity(1);
        setShowBill(false);
    }

    return (
        <div className="space-y-6">
            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-5 text-xl font-semibold">New Sale</h3>

                <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                    <div>
                        <label className="mb-2 block text-sm">Customer Name</label>
                        <input
                            type="text"
                            placeholder="Enter customer name"
                            value={customer}
                            onChange={(e) => setCustomer(e.target.value)}
                            className="w-full rounded border border-gray-300 p-3"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm">Select Book</label>
                        <select
                            value={selectedBook}
                            onChange={(e) => setSelectedBook(e.target.value)}
                            className="w-full rounded border border-gray-300 bg-white p-3"
                        >
                            <option value="">Choose a book</option>
                            {availableBooks.map((book) => (
                                <option key={book.id} value={book.id}>
                                    {book.title} - Rs. {book.price}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm">Quantity</label>
                        <input
                            type="number"
                            min="1"
                            step="1"
                            value={quantity}
                            onChange={(e) => setQuantity(Number(e.target.value))}
                            className="w-full rounded border border-gray-300 p-3"
                        />
                    </div>
                </div>

                <button
                    onClick={addToCart}
                    className="mt-5 rounded bg-blue-600 px-5 py-3 text-white hover:bg-blue-700"
                >
                    + Add to Cart
                </button>
            </section>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="mb-5 text-xl font-semibold">Shopping Cart</h3>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-100">
                        <tr>
                            <th className="p-4">Book Name</th>
                            <th className="p-4">Price</th>
                            <th className="p-4">Quantity</th>
                            <th className="p-4">Subtotal</th>
                            <th className="p-4">Action</th>
                        </tr>
                        </thead>

                        <tbody>
                        {cart.length === 0 ? (
                            <tr>
                                <td
                                    colSpan={5}
                                    className="p-8 text-center text-gray-500"
                                >
                                    No books added to cart.
                                </td>
                            </tr>
                        ) : (
                            cart.map((item) => (
                                <tr key={item.id} className="border-t border-gray-200">
                                    <td className="p-4">{item.title}</td>
                                    <td className="p-4">Rs. {item.price}</td>
                                    <td className="p-4">{item.quantity}</td>
                                    <td className="p-4">
                                        Rs. {item.price * item.quantity}
                                    </td>
                                    <td className="p-4">
                                        <button
                                            onClick={() => removeFromCart(item.id)}
                                            className="rounded bg-red-600 px-3 py-2 text-white hover:bg-red-700"
                                        >
                                            Remove
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
                    <h4 className="text-xl font-bold">Total: Rs. {total}</h4>

                    <div className="flex gap-3">
                        <button
                            onClick={newSale}
                            className="rounded border border-gray-300 px-4 py-2 hover:bg-gray-100"
                        >
                            New Sale
                        </button>
                        <button
                            onClick={generateBill}
                            className="rounded bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                        >
                            Generate Bill
                        </button>
                    </div>
                </div>
            </section>

            {showBill && (
                <section className="rounded border border-gray-200 bg-white p-6">
                    <h3 className="text-center text-2xl font-bold">Qadri Books</h3>
                    <p className="mt-1 text-center text-gray-500">
                        Customer Bill Preview
                    </p>

                    <p className="mt-6">
                        <strong>Customer:</strong> {customer.trim() || "Walk-in Customer"}
                    </p>

                    <div className="mt-4 space-y-2">
                        {cart.map((item) => (
                            <div
                                key={item.id}
                                className="flex justify-between border-b border-gray-200 py-2"
                            >
                <span>
                  {item.title} × {item.quantity}
                </span>
                                <span>Rs. {item.price * item.quantity}</span>
                            </div>
                        ))}
                    </div>

                    <h4 className="mt-5 text-right text-xl font-bold">
                        Grand Total: Rs. {total}
                    </h4>
                </section>
            )}
        </div>
    );
}
