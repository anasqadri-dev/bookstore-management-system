
"use client";

import { useCallback, useEffect, useState } from "react";
import { apiRequest } from "../lib/api";

type Book = {
    id: number;
    sku: string;
    minStock: number;
    title: string;
    author: string;
    price: number;
    quantity: number;
};

export default function BooksInventory() {
    const [books, setBooks] = useState<Book[]>([]);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [apiError, setApiError] = useState("");
    const loadBooks = useCallback(async () => {
        try { setBooks(await apiRequest<Book[]>("/api/products")); setApiError(""); }
        catch (error) { setApiError(error instanceof Error ? error.message : "Unable to load books."); }
        finally { setLoading(false); }
    }, []);
    useEffect(() => {
        apiRequest<Book[]>("/api/products")
            .then((products) => { setBooks(products); setApiError(""); })
            .catch((error: unknown) => { setApiError(error instanceof Error ? error.message : "Unable to load books."); })
            .finally(() => { setLoading(false); });
    }, []);
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);
    const [formError, setFormError] = useState("");

    const [form, setForm] = useState({
        title: "",
        author: "",
        price: "",
        quantity: "",
    });

    function resetForm() {
        setFormError("");
        setForm({
            title: "",
            author: "",
            price: "",
            quantity: "",
        });
        setEditId(null);
        setShowForm(false);
    }

    async function saveBook(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();

        const title = form.title.trim();
        const author = form.author.trim();
        const price = Number(form.price);
        const quantity = Number(form.quantity);

        if (!title || !author || !form.price || !form.quantity) {
            setFormError("Please fill all fields.");
            return;
        }

        if (
            !Number.isFinite(price) ||
            price <= 0 ||
            !Number.isSafeInteger(quantity) ||
            quantity < 0
        ) {
            setFormError("Enter a valid price and quantity.");
            return;
        }

        setBusy(true);
        setFormError("");
        try {
            const existing = books.find((book) => book.id === editId);
            await apiRequest(editId !== null ? `/api/products/${editId}` : "/api/products", {
                method: editId !== null ? "PUT" : "POST",
                body: JSON.stringify({
                    sku: existing?.sku ?? `BOOK-${Date.now()}`,
                    title, author, price, quantity,
                    minStock: existing?.minStock ?? 0,
                }),
            });
            await loadBooks();
            resetForm();
        } catch (error) {
            setFormError(error instanceof Error ? error.message : "Unable to save book.");
        } finally {
            setBusy(false);
        }
    }

    function editBook(book: Book) {
        setForm({
            title: book.title,
            author: book.author,
            price: String(book.price),
            quantity: String(book.quantity),
        });

        setEditId(book.id);
        setShowForm(true);
    }

    async function deleteBook(id: number) {
        if (!window.confirm("Are you sure you want to delete this book?")) return;
        setBusy(true);
        try {
            await apiRequest(`/api/products/${id}`, { method: "DELETE" });
            await loadBooks();
            if (editId === id) resetForm();
        } catch (error) {
            setApiError(error instanceof Error ? error.message : "Unable to delete book.");
        } finally {
            setBusy(false);
        }
    }

    const filteredBooks = books.filter(
        (book) =>
            book.title.toLowerCase().includes(search.toLowerCase()) ||
            book.author.toLowerCase().includes(search.toLowerCase())
    );

    const inputStyle =
        "w-full rounded border border-gray-300 bg-white p-3 text-gray-900 outline-none focus:border-blue-500";

    return (
        <div>
            {/* Heading */}
            <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
                <h3 className="text-xl font-semibold">
                    Books List
                </h3>

                <button
                    onClick={() => {
                        if (showForm) {
                            resetForm();
                        } else {
                            resetForm();
                            setShowForm(true);
                        }
                    }}
                    className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
                >
                    {showForm ? "Cancel" : "+ Add Book"}
                </button>
            </div>

            {apiError && <p role="alert" className="mb-4 text-red-700">{apiError} <button type="button" className="underline" onClick={() => void loadBooks()}>Retry</button></p>}
            {/* Add / Edit Form */}
            {showForm && (
                <form
                    onSubmit={saveBook}
                    className="mb-6 rounded border border-gray-200 bg-white p-6"
                >
                    <h4 className="mb-5 text-lg font-semibold">
                        {editId !== null ? "Edit Book" : "Add New Book"}
                    </h4>

                    {formError && (
                        <p id="inventory-form-error" role="alert" className="mb-4 text-sm text-red-700">
                            {formError}
                        </p>
                    )}
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                        <div>
                            <label htmlFor="inventory-book-name" className="mb-2 block text-sm">
                                Book Name
                            </label>
                            <input
                                id="inventory-book-name"
                                aria-describedby={formError ? "inventory-form-error" : undefined}
                                aria-invalid={Boolean(formError)}
                                type="text"
                                value={form.title}
                                onChange={(e) =>
                                    setForm({ ...form, title: e.target.value })
                                }
                                className={inputStyle}
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="inventory-author" className="mb-2 block text-sm">
                                Author
                            </label>
                            <input
                                id="inventory-author"
                                aria-describedby={formError ? "inventory-form-error" : undefined}
                                aria-invalid={Boolean(formError)}
                                type="text"
                                value={form.author}
                                onChange={(e) =>
                                    setForm({ ...form, author: e.target.value })
                                }
                                className={inputStyle}
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="inventory-price" className="mb-2 block text-sm">
                                Price (Rs.)
                            </label>
                            <input
                                id="inventory-price"
                                aria-describedby={formError ? "inventory-form-error" : undefined}
                                aria-invalid={Boolean(formError)}
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={form.price}
                                onChange={(e) =>
                                    setForm({ ...form, price: e.target.value })
                                }
                                className={inputStyle}
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="inventory-quantity"  className="mb-2 block text-sm">
                                Quantity
                            </label>
                            <input
                                id="inventory-quantity"
                                aria-describedby={formError ? "inventory-form-error" : undefined}
                                aria-invalid={Boolean(formError)}
                                type="number"
                                min="0"
                                step="1"
                                value={form.quantity}
                                onChange={(e) =>
                                    setForm({ ...form, quantity: e.target.value })
                                }
                                className={inputStyle}
                                required
                            />
                        </div>
                    </div>

                    <div className="mt-6 flex gap-3">
                        <button
                            type="submit"
                            disabled={busy}
                            className="rounded bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
                        >
                            {editId !== null ? "Update Book" : "Save Book"}
                        </button>

                        <button
                            type="button"
                            onClick={resetForm}
                            className="rounded border border-gray-300 px-5 py-2 hover:bg-gray-100"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {/* Search */}
            <div className="mb-5">
                <label htmlFor="search-books" className="sr-only">Search books by name or author</label>
                <input
                    type="search"
                    id="search-books"
                    placeholder="Search by book name or author"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="w-full rounded border border-gray-300 bg-white p-3 outline-none focus:border-blue-500 md:w-80"
                />
            </div>

            {/* Books Table */}
            <div className="overflow-x-auto rounded border border-gray-200 bg-white">
                <table className="w-full text-left text-sm">
                    <thead className="bg-[#f3f4f6]">
                    <tr>
                        <th className="p-4">Book Name</th>
                        <th className="p-4">Author</th>
                        <th className="p-4">Price</th>
                        <th className="p-4">Quantity</th>
                        <th className="p-4">Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    {filteredBooks.length === 0 ? (
                        <tr>
                            <td colSpan={5} className="p-8 text-center text-gray-500">
                                {loading ? "Loading books..." : books.length === 0
                                    ? "No books added yet."
                                    : "No matching books found."}
                            </td>
                        </tr>
                    ) : (
                        filteredBooks.map((book) => (
                            <tr
                                key={book.id}
                                className="border-t border-gray-200 hover:bg-gray-50"
                            >
                                <td className="p-4">{book.title}</td>
                                <td className="p-4">{book.author}</td>
                                <td className="p-4">Rs. {book.price}</td>
                                <td className="p-4">{book.quantity}</td>
                                <td className="p-4">
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => editBook(book)}
                                            className="rounded bg-blue-600 px-3 py-2 text-white hover:bg-blue-700"
                                        >
                                            Edit
                                        </button>

                                        <button
                                            disabled={busy}
                                            onClick={() => void deleteBook(book.id)}
                                            className="rounded bg-red-600 px-3 py-2 text-white hover:bg-red-700"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>
            </div>

            <p className="mt-5 text-sm text-gray-600">
                Total Books: {books.length}
            </p>
        </div>
    );
}
