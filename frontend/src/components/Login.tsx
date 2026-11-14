"use client";

import { useState, type FormEvent } from "react";
import { apiRequest, type AuthenticatedUser } from "../lib/api";

type LoginProps = {
    onLogin: (user: AuthenticatedUser) => void;
    notice?: string;
};

export default function Login({ onLogin, notice = "" }: LoginProps) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleLogin(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isSubmitting) return;
        setError("");
        setIsSubmitting(true);
        try {
            const user = await apiRequest<AuthenticatedUser>("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({ username: username.trim(), password }),
            });
            onLogin(user);
        } catch (cause) {
            setError(cause instanceof Error
                ? cause.message
                : "Sign in failed. Check your credentials and try again.");
        } finally {
            setIsSubmitting(false);
        }
    }

    const fieldClass = "mt-2 block w-full rounded-md border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-100";

    return (
        <main className="flex min-h-dvh items-center justify-center bg-[#f3f4f6] px-4 py-8 text-[#111827] sm:px-6">
            <div className="w-full max-w-5xl overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm md:grid md:min-h-[570px] md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
                <aside className="flex flex-col justify-between gap-10 bg-[#030712] px-7 py-8 text-white sm:px-10 sm:py-10">
                    <div className="flex items-center gap-3">
                        <span aria-hidden="true" className="flex h-11 w-11 items-center justify-center rounded-md bg-slate-800 text-2xl">▤</span>
                        <div>
                            <h1 className="text-2xl font-bold leading-tight">Book Store</h1>
                            <p className="mt-1 text-sm text-gray-400">Management System</p>
                        </div>
                    </div>
                    <div className="hidden md:block">
                        <p className="mb-3 text-xs font-semibold uppercase tracking-[0.2em] text-blue-300">Your workspace</p>
                        <h2 className="max-w-xs text-3xl font-bold leading-tight">Manage your bookstore with confidence.</h2>
                        <p className="mt-5 max-w-xs text-sm leading-7 text-gray-400">
                            One organized dashboard for books, sales, suppliers and school orders.
                        </p>
                        <div className="mt-8 space-y-3 text-sm text-gray-300">
                            {["Books Inventory", "Sales & Purchase Orders", "Reports & Store Management"].map((feature) => (
                                <div key={feature} className="flex items-center gap-3">
                                    <span aria-hidden="true" className="flex h-6 w-6 items-center justify-center rounded-md bg-slate-800 text-blue-300">✓</span>
                                    <span>{feature}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                    <p className="hidden text-xs text-gray-500 md:block">Bookstore Management System</p>
                </aside>

                <section className="flex items-center justify-center px-6 py-10 sm:px-12 md:py-14" aria-labelledby="login-heading">
                    <div className="w-full max-w-sm">
                        <p className="text-xs font-bold uppercase tracking-[0.18em] text-blue-700">Secure access</p>
                        <h2 id="login-heading" className="mt-3 text-3xl font-bold tracking-tight">Welcome back</h2>
                        <p className="mt-2 text-sm leading-6 text-gray-500">
                            Sign in with your bookstore account to continue.
                        </p>

                        {notice && <p role="status" className="mt-5 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{notice}</p>}
                        <form onSubmit={(event) => void handleLogin(event)} className="mt-8 space-y-5" aria-busy={isSubmitting}>
                            <div>
                                <label htmlFor="login-username" className="block text-sm font-medium text-gray-700">Username</label>
                                <input id="login-username" name="username" type="text" required autoComplete="username"
                                    value={username} disabled={isSubmitting} placeholder="Enter your username"
                                    onChange={(event) => { setUsername(event.target.value); setError(""); }}
                                    aria-invalid={Boolean(error)} aria-describedby={error ? "login-error" : undefined}
                                    className={fieldClass} />
                            </div>
                            <div>
                                <label htmlFor="login-password" className="block text-sm font-medium text-gray-700">Password</label>
                                <div className="relative">
                                    <input id="login-password" name="password" type={showPassword ? "text" : "password"}
                                        required autoComplete="current-password" value={password} disabled={isSubmitting}
                                        placeholder="Enter your password"
                                        onChange={(event) => { setPassword(event.target.value); setError(""); }}
                                        aria-invalid={Boolean(error)} aria-describedby={error ? "login-error" : undefined}
                                        className={fieldClass + " pr-20"} />
                                    <button type="button" disabled={isSubmitting}
                                        onClick={() => setShowPassword((previous) => !previous)}
                                        aria-label={showPassword ? "Hide password" : "Show password"}
                                        aria-pressed={showPassword}
                                        className="absolute inset-y-0 right-3 top-2 px-2 text-sm font-medium text-blue-700 hover:text-blue-900 focus-visible:rounded focus-visible:outline focus-visible:outline-2 focus-visible:outline-blue-600 disabled:opacity-50">
                                        {showPassword ? "Hide" : "Show"}
                                    </button>
                                </div>
                            </div>
                            {error && <p id="login-error" role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</p>}
                            <button type="submit" disabled={isSubmitting}
                                className="w-full rounded-md bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-60">
                                {isSubmitting ? "Signing in..." : "Sign In"}
                            </button>
                        </form>
                        <p className="mt-7 text-center text-xs text-gray-500">
                            Authorized bookstore staff only
                        </p>
                    </div>
                </section>
            </div>
        </main>
    );
}
