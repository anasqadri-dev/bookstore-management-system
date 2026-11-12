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
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export async function apiRequest<T>(
    path: string,
    options: RequestInit = {},
): Promise<T> {
    const headers = new Headers(options.headers);

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
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
