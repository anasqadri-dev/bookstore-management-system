"use client";

import { useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";

type AdminSetupProps = {
    onComplete: () => void;
};

export default function AdminSetup({ onComplete }: AdminSetupProps) {
    const [fullName, setFullName] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isSubmitting) return;
        setError("");

        if (password !== confirmPassword) {
            setError("The passwords do not match.");
            return;
        }

        const passwordBytes = new TextEncoder().encode(password).length;
        if (passwordBytes < 8 || passwordBytes > 72) {
            setError("Use a password containing 8 to 72 UTF-8 bytes.");
            return;
        }

        setIsSubmitting(true);
        try {
            await apiRequest<void>("/api/setup", {
                method: "POST",
                body: JSON.stringify({
                    fullName: fullName.trim(),
                    username: username.trim(),
                    password,
                }),
            });
            onComplete();
        } catch (cause) {
            const message = cause instanceof Error
                ? cause.message
                : "The Admin account could not be created. Try again.";

            // Another local window may have completed setup while this form was open.
            try {
                const status = await apiRequest<{ setupRequired: boolean }>(
                    "/api/setup/status",
                    { cache: "no-store" },
                );
                if (!status.setupRequired) {
                    onComplete();
                    return;
                }
            } catch {
                // Preserve the original actionable error if status cannot be checked.
            }
            setError(message);
        } finally {
            setIsSubmitting(false);
        }
    }

    const fieldClass = "mt-2 block w-full rounded-md border border-gray-300 bg-white px-4 py-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-100 disabled:bg-gray-100";

    return (
        <main className="flex min-h-dvh items-center justify-center bg-[#f3f4f6] px-4 py-8 text-[#111827] sm:px-6">
            <section className="w-full max-w-xl overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
                <div className="bg-[#030712] px-7 py-8 text-white sm:px-10">
                    <p className="text-xs font-bold uppercase tracking-[0.2em] text-blue-300">
                        First-time setup
                    </p>
                    <h1 className="mt-3 text-2xl font-bold sm:text-3xl">
                        Create your Admin account
                    </h1>
                    <p className="mt-3 max-w-lg text-sm leading-6 text-gray-300">
                        Set up the owner account for this bookstore. This step is available
                        only while first-time setup has not been completed.
                    </p>
                </div>

                <form onSubmit={(event) => void handleSubmit(event)}
                    className="space-y-5 px-7 py-8 sm:px-10"
                    aria-busy={isSubmitting}>
                    <div>
                        <label htmlFor="setup-full-name" className="block text-sm font-medium text-gray-700">
                            Owner full name
                        </label>
                        <input id="setup-full-name" name="fullName" required maxLength={150}
                            autoComplete="name" value={fullName} disabled={isSubmitting}
                            onChange={(event) => setFullName(event.target.value)}
                            placeholder="Enter your full name" className={fieldClass} />
                    </div>

                    <div>
                        <label htmlFor="setup-username" className="block text-sm font-medium text-gray-700">
                            Admin username
                        </label>
                        <input id="setup-username" name="username" required maxLength={100}
                            autoComplete="username" value={username} disabled={isSubmitting}
                            onChange={(event) => setUsername(event.target.value)}
                            placeholder="Choose a username" className={fieldClass} />
                    </div>

                    <div>
                        <label htmlFor="setup-password" className="block text-sm font-medium text-gray-700">
                            Password
                        </label>
                        <input id="setup-password" name="password" type="password" required
                            autoComplete="new-password" value={password} disabled={isSubmitting}
                            onChange={(event) => setPassword(event.target.value)}
                            placeholder="Create a strong password" className={fieldClass} />
                        <p className="mt-2 text-xs text-gray-500">
                            Use 8–72 UTF-8 bytes. Passwords are securely hashed before storage.
                        </p>
                    </div>

                    <div>
                        <label htmlFor="setup-confirm-password" className="block text-sm font-medium text-gray-700">
                            Confirm password
                        </label>
                        <input id="setup-confirm-password" name="confirmPassword" type="password"
                            required autoComplete="new-password" value={confirmPassword}
                            disabled={isSubmitting}
                            onChange={(event) => setConfirmPassword(event.target.value)}
                            placeholder="Enter the password again" className={fieldClass} />
                    </div>

                    {error && (
                        <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                            {error}
                        </p>
                    )}

                    <div className="rounded-md border border-blue-100 bg-blue-50 p-4 text-sm leading-6 text-blue-900">
                        <strong>One-time setup:</strong> after this account is created, this
                        setup form is permanently disabled for this database. Use the normal
                        sign-in screen for future access.
                    </div>

                    <button type="submit" disabled={isSubmitting}
                        className="w-full rounded-md bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-60">
                        {isSubmitting ? "Creating Admin account..." : "Create Admin account"}
                    </button>
                </form>
            </section>
        </main>
    );
}
