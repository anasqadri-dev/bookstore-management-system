export declare class ApiError extends Error {
    status: number | null;
    code: string | null;
    details: unknown;
}
export declare function createApiClient(options: {
    baseUrl: string;
    fetchImpl?: typeof fetch;
}): {
    request<T>(path: string, options?: RequestInit): Promise<T>;
};
