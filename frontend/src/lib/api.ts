import { createApiClient, ApiError } from "./api-core.mjs";

export type UserRole = "ADMIN" | "STAFF";

export interface AuthenticatedUser {
    id: number;
    username: string;
    fullName: string;
    role: UserRole;
}

const API_BASE_URL = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://127.0.0.1:8080"
).replace(/\\/+$/, "");

const client = createApiClient({ baseUrl: API_BASE_URL });

export { ApiError };

export async function apiRequest<T>(
    path: string,
    options: RequestInit = {},
): Promise<T> {
    return client.request<T>(path, options);
}
