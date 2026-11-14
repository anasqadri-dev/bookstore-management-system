import test from "node:test";
import assert from "node:assert/strict";
import { ApiError, createApiClient } from "../src/lib/api-core.mjs";

function response(status, body) {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" },
    });
}

test("parses a successful API envelope and includes session credentials", async () => {
    let requestOptions;
    const client = createApiClient({
        baseUrl: "http://127.0.0.1:8080/",
        fetchImpl: async (_url, options) => {
            requestOptions = options;
            return response(200, { success: true, message: "ok", data: [{ id: 1 }] });
        },
    });

    assert.deepEqual(await client.request("/api/products"), [{ id: 1 }]);
    assert.equal(requestOptions.credentials, "include");
    assert.equal(requestOptions.headers.get("Accept"), "application/json");
});

test("adds a session-bound CSRF token to state-changing requests", async () => {
    const calls = [];
    const client = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async (url, options) => {
            calls.push({ url, options });
            return url.endsWith("/api/auth/csrf")
                ? response(200, { success: true, message: "ok", data: { token: "csrf-test" } })
                : response(201, { success: true, message: "created", data: { id: 9 } });
        },
    });

    assert.deepEqual(await client.request("/api/products", {
        method: "POST",
        body: JSON.stringify({ title: "Book" }),
    }), { id: 9 });
    assert.equal(calls.length, 2);
    assert.equal(calls[0].options.credentials, "include");
    assert.equal(calls[1].options.headers.get("X-CSRF-TOKEN"), "csrf-test");
    assert.equal(calls[1].options.headers.get("Content-Type"), "application/json");
});

test("does not request a CSRF token for login", async () => {
    const urls = [];
    const client = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async (url) => {
            urls.push(url);
            return response(200, { success: true, message: "ok", data: { id: 1 } });
        },
    });
    await client.request("/api/auth/login", { method: "POST", body: "{}" });
    assert.deepEqual(urls, ["http://127.0.0.1:8080/api/auth/login"]);
});

for (const status of [400, 401, 403, 404, 409]) {
    test(`preserves HTTP ${status} for callers`, async () => {
        const client = createApiClient({
            baseUrl: "http://127.0.0.1:8080",
            fetchImpl: async () => response(status, {
                success: false, message: `failure ${status}`, data: { field: "invalid" },
            }),
        });
        await assert.rejects(client.request("/api/products"), (error) => {
            assert.ok(error instanceof ApiError);
            assert.equal(error.status, status);
            assert.equal(error.message, `failure ${status}`);
            assert.deepEqual(error.details, { field: "invalid" });
            return true;
        });
    });
}

test("distinguishes malformed JSON and malformed envelopes", async () => {
    const malformedJson = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async () => new Response("not-json", { status: 200 }),
    });
    await assert.rejects(malformedJson.request("/api/products"), (error) =>
        error.code === "INVALID_RESPONSE");

    const malformedEnvelope = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async () => response(200, { message: "missing success flag" }),
    });
    await assert.rejects(malformedEnvelope.request("/api/products"), (error) =>
        error.code === "INVALID_ENVELOPE");
});

test("normalizes network failures", async () => {
    const client = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async () => { throw new TypeError("connection refused"); },
    });
    await assert.rejects(client.request("/api/products"), (error) =>
        error instanceof ApiError && error.code === "NETWORK_ERROR");
});

test("requires a data field even for successful responses", async () => {
    const client = createApiClient({
        baseUrl: "http://127.0.0.1:8080",
        fetchImpl: async () => response(200, { success: true, message: "ok" }),
    });
    await assert.rejects(client.request("/api/products"), (error) =>
        error.code === "MISSING_DATA");
});
