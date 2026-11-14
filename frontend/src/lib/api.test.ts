import { afterEach, describe, expect, it, vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetModules();
});

async function loadApi(fetchImpl: typeof fetch) {
  vi.resetModules();
  vi.stubGlobal("fetch", fetchImpl);
  const module = await import("./api");
  return module.apiRequest;
}

const envelope = (status: number, data: unknown, message = "OK") => ({
  ok: status >= 200 && status < 300,
  status,
  json: async () => ({ success: status >= 200 && status < 300, message, data }),
});

describe("apiRequest wrapper", () => {
  it("returns the data envelope on success", async () => {
    const fetchMock = vi.fn().mockResolvedValue(envelope(200, { id: 7 }));
    const apiRequest = await loadApi(fetchMock as unknown as typeof fetch);
    await expect(apiRequest("/api/auth/me")).resolves.toEqual({ id: 7 });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://127.0.0.1:8080/api/auth/me",
      expect.objectContaining({ credentials: "include", method: "GET" }),
    );
  });

  it("maps authorization failures to a safe user-facing message", async () => {
    const fetchMock = vi.fn().mockResolvedValue(envelope(403, null, "Access denied"));
    const apiRequest = await loadApi(fetchMock as unknown as typeof fetch);
    await expect(apiRequest("/api/users")).rejects.toMatchObject({
      status: 403,
      message: "You do not have permission to perform this action.",
    });
  });

  it("normalizes a network failure", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("offline"));
    const apiRequest = await loadApi(fetchMock as unknown as typeof fetch);
    await expect(apiRequest("/api/auth/me")).rejects.toMatchObject({
      code: "NETWORK_ERROR",
    });
  });

  it("sends credentials and JSON headers for a login request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(envelope(200, { username: "admin" }));
    const apiRequest = await loadApi(fetchMock as unknown as typeof fetch);
    await apiRequest("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: "admin", password: "secret" }),
    });
    const options = fetchMock.mock.calls[0][1] as RequestInit;
    expect(options.credentials).toBe("include");
    expect(new Headers(options.headers).get("Content-Type")).toBe("application/json");
  });
});
