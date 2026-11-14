export type UserRole = "ADMIN" | "STAFF";

export interface AuthenticatedUser {
    id: number;
    username: string;
    fullName: string;
    role: UserRole;
}

interface ApiEnvelope<T> {
    success: boolean;
    message: string;
    data: T | null;
}

const API_BASE_URL = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://127.0.0.1:8080"
).replace(/\/+$/, "");

async function fetchCsrfToken(): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
        method: "GET",
        credentials: "include",
        headers: { Accept: "application/json" },
        cache: "no-store",
    });
    const envelope = await response.json().catch(() => null) as
        ApiEnvelope<{ token: string }> | null;

    if (!response.ok || !envelope?.success || !envelope.data?.token) {
        throw new Error(
            envelope?.message || "Unable to establish a secure request token.",
        );
    }

    return envelope.data.token;
}

export async function apiRequest<T>(
    path: string,
    options: RequestInit = {},
): Promise<T> {
    const headers = new Headers(options.headers);
    const method = (options.method ?? "GET").toUpperCase();

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const requiresCsrf = ["POST", "PUT", "PATCH", "DELETE"].includes(method)
        && path !== "/api/auth/login";

    if (requiresCsrf) {
        headers.set("X-CSRF-TOKEN", await fetchCsrfToken());
    }

    let response: Response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers,
            credentials: "include",
        });
    } catch {
        throw new Error(
            "Cannot connect to the bookstore server. Make sure the local backend is running and try again.",
        );
    }

    const envelope = await response.json().catch(() => null) as ApiEnvelope<T> | null;

    if (!response.ok || !envelope?.success) {
        throw new Error(
            envelope?.message || `The request failed (HTTP ${response.status}).`,
        );
    }

    return envelope.data as T;
}
