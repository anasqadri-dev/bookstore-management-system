"use client";

import { useCallback, useEffect, useState, type ReactNode } from "react";
import { apiRequest } from "../lib/api";
import { isBackendReady } from "../lib/backend-health.mjs";

type ConnectionState = "connecting" | "ready" | "unavailable";

export default function BackendReadiness({ children }: { children: ReactNode }) {
    const [status, setStatus] = useState<ConnectionState>("connecting");
    const [attempt, setAttempt] = useState(0);

    const retry = useCallback(() => {
        setStatus("connecting");
        setAttempt((previous) => previous + 1);
    }, []);

    useEffect(() => {
        let active = true;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);

        apiRequest<{ status?: string; database?: string }>("/api/health", { signal: controller.signal, cache: "no-store" })
            .then((health) => {
                if (active) setStatus(isBackendReady(health) ? "ready" : "unavailable");
            })
            .catch(() => {
                if (active) setStatus("unavailable");
            })
            .finally(() => clearTimeout(timeout));

        return () => {
            active = false;
            clearTimeout(timeout);
            controller.abort();
        };
    }, [attempt]);

    if (status === "ready") return <>{children}</>;

    return (
        <main className="flex min-h-screen items-center justify-center bg-gray-100 p-6">
            <section role="status" aria-live="polite" className="w-full max-w-md rounded-xl border bg-white p-8 text-center shadow-sm">
                <h1 className="text-xl font-semibold text-gray-900">Bookstore server</h1>
                {status === "connecting" ? (
                    <p className="mt-4 text-gray-600">Connecting to the local bookstore server...</p>
                ) : (
                    <>
                        <p className="mt-4 text-red-700">
                            The local bookstore server is unavailable. Your saved records have not been loaded.
                            Start the Spring Boot backend and try again.
                        </p>
                        <button
                            type="button"
                            onClick={retry}
                            className="mt-6 rounded-md bg-slate-800 px-5 py-3 text-white hover:bg-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-800"
                        >
                            Retry connection
                        </button>
                    </>
                )}
            </section>
        </main>
    );
}
