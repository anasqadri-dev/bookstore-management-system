export class ApiError extends Error {
    constructor(message, { status = null, code = null, details = null } = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

async function readEnvelope(response, operation) {
    let envelope;
    try {
        envelope = await response.json();
    } catch {
        throw new ApiError(
            `The server returned an invalid response while ${operation} (HTTP ${response.status}).`,
            { status: response.status, code: "INVALID_RESPONSE" },
        );
    }

    if (!envelope || typeof envelope !== "object" || typeof envelope.success !== "boolean") {
        throw new ApiError("The server returned an invalid API response.", {
            status: response.status,
            code: "INVALID_ENVELOPE",
        });
    }

    if (!response.ok || !envelope.success) {
        throw new ApiError(
            typeof envelope.message === "string" && envelope.message.trim()
                ? envelope.message
                : `The request failed (HTTP ${response.status}).`,
            {
                status: response.status,
                code: envelope.code ?? null,
                details: envelope.data ?? null,
            },
        );
    }

    if (!Object.prototype.hasOwnProperty.call(envelope, "data")) {
        throw new ApiError("The server response did not include a data field.", {
            status: response.status,
            code: "MISSING_DATA",
        });
    }

    return envelope.data;
}

export function createApiClient({ baseUrl, fetchImpl = globalThis.fetch }) {
    const normalizedBaseUrl = baseUrl.replace(/\\/+$/, "");

    async function request(path, options = {}) {
        const method = (options.method ?? "GET").toUpperCase();
        const headers = new Headers(options.headers);

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }
        headers.set("Accept", "application/json");

        const requiresCsrf = ["POST", "PUT", "PATCH", "DELETE"].includes(method)
            && path !== "/api/auth/login";

        if (requiresCsrf) {
            let csrfResponse;
            try {
                csrfResponse = await fetchImpl(`${normalizedBaseUrl}/api/auth/csrf`, {
                    method: "GET",
                    credentials: "include",
                    headers: { Accept: "application/json" },
                    cache: "no-store",
                });
            } catch {
                throw new ApiError(
                    "Cannot connect to the bookstore server to establish a secure request token.",
                    { code: "NETWORK_ERROR" },
                );
            }
            const csrf = await readEnvelope(csrfResponse, "establishing a secure request token");
            if (!csrf || typeof csrf.token !== "string" || !csrf.token) {
                throw new ApiError("The server did not provide a valid CSRF token.", {
                    status: csrfResponse.status,
                    code: "INVALID_CSRF_TOKEN",
                });
            }
            headers.set("X-CSRF-TOKEN", csrf.token);
        }

        let response;
        try {
            response = await fetchImpl(`${normalizedBaseUrl}${path}`, {
                ...options,
                method,
                headers,
                credentials: "include",
            });
        } catch {
            throw new ApiError(
                "Cannot connect to the bookstore server. Make sure the local backend is running and try again.",
                { code: "NETWORK_ERROR" },
            );
        }

        return readEnvelope(response, `${method} ${path}`);
    }

    return { request };
}
