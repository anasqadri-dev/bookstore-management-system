
import { describe, it, expect, vi, afterEach } from "vitest";
import { apiRequest } from "./api";

afterEach(() => {
    vi.restoreAllMocks();
     vi.unstubAllGlobals();
});

describe("apiRequest", () => {

    // FIRST TEST
    it("should return data when the API request is successful", async () => {
        const mockData = {
            id: 1,
            username: "waleed",
            fullName: "Test User",
            role: "ADMIN"
        };

        vi.stubGlobal(
            "fetch",
            vi.fn().mockResolvedValue({
                ok: true,
                status: 200,
                json: async () => ({
                    success: true,
                    message: "Login successful",
                    data: mockData,
                }),
            }),
        );

        const result = await apiRequest("/api/auth/me");

        expect(result).toEqual(mockData);
    });

    // SECOND TEST — separate from the first
    it("should throw an error when the API returns success false", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn().mockResolvedValue({
                ok: false,
                status: 400,
                json: async () => ({
                    success: false,
                    message: "Invalid request",
                    data: null,
                }),
            }),
        );

        await expect(
            apiRequest("/api/auth/login"),
        ).rejects.toThrow("Invalid request");
    });

    it("should throw an error when the response status is 403", async () => {
    vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue({
            ok: false,
            status: 403,
            json: async () => ({
                success: false,
                message: "Access denied",
                data: null,
            }),
        }),
    );

    await expect(
        apiRequest("/api/admin/users"),
    ).rejects.toThrow("Access denied");
});

    it("should throw an error when the server is unreachable", async () => {
    vi.stubGlobal(
        "fetch",
        vi.fn().mockRejectedValue(new Error("Network Error")),
    );

    await expect(
        apiRequest("/api/auth/me"),
    ).rejects.toThrow(
        "Cannot connect to the bookstore server. Make sure the local backend is running and try again."
    );
});

    it("should include credentials in the API request", async () => {
    const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
            success: true,
            message: "Success",
            data: {},
        }),
    });

    vi.stubGlobal("fetch", mockFetch);

    await apiRequest("/api/auth/me");

    expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/auth/me",
        expect.objectContaining({
            credentials: "include",
        }),
    );
});

    it("should set Content-Type to application/json when a request has a body", async () => {
    const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
            success: true,
            message: "Success",
            data: {},
        }),
    });

    vi.stubGlobal("fetch", mockFetch);

    await apiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
            username: "waleed",
            password: "test123",
        }),
    });

    const requestOptions = mockFetch.mock.calls[0][1];

    expect(requestOptions.headers.get("Content-Type")).toBe(
        "application/json",
    );
});

    it("should preserve an explicitly provided Content-Type header", async () => {
    const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
            success: true,
            message: "Success",
            data: {},
        }),
    });

    vi.stubGlobal("fetch", mockFetch);

    await apiRequest("/api/auth/me", {
        method: "POST",
        body: "{}",
        headers: {
            "Content-Type": "application/custom",
        },
    });

    const requestOptions = mockFetch.mock.calls[0][1] as RequestInit;

    expect(
        new Headers(requestOptions.headers).get("Content-Type"),
    ).toBe("application/custom");
});

    it("should send the request to the correct API endpoint", async () => {
    const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({
            success: true,
            message: "Success",
            data: {},
        }),
    });

    vi.stubGlobal("fetch", mockFetch);

    await apiRequest("/api/auth/me");

    expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/auth/me",
        expect.any(Object),
    );
});

    it("should throw an HTTP error when the response contains invalid JSON", async () => {
    vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
            json: async () => {
                throw new Error("Invalid JSON");
            },
        }),
    );

    await expect(
        apiRequest("/api/auth/me"),
    ).rejects.toThrow("The request failed (HTTP 500).");
});

    it("should throw an error when the response status is 401", async () => {
    vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
            json: async () => ({
                success: false,
                message: "",
                data: null,
            }),
        }),
    );

    await expect(
        apiRequest("/api/auth/me"),
    ).rejects.toThrow("The request failed (HTTP 401).");
});

});
