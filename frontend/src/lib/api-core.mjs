export class ApiError extends Error {
    constructor(message, { status = null, code = null, details = null } = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

function userFacingMessage(status, serverMessage) {
    if (status === 401) return "Your session has expired or you are not signed in. Please sign in again.";
    if (status === 403) return "You do not have permission to perform this action.";
    if (status === 404) return "The requested record or resource could not be found.";
    if (status === 409) return serverMessage || "This action conflicts with the current saved data. Refresh and try again.";
    if (status >= 500) return "The local bookstore server encountered an error. Please try again.";
    if (status === 400) return serverMessage || "Some of the entered information is invalid. Review the form and try again.";
    return serverMessage || `The request failed (HTTP ${status}).`;
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
        const serverMessage = typeof envelope.message === "string" && envelope.message.trim()
            ? envelope.message
            : "";
        throw new ApiError(userFacingMessage(response.status, serverMessage), {
            status: response.status,
            code: envelope.code ?? null,
            details: envelope.data ?? null,
        });
    }

    if (!Object.prototype.hasOwnProperty.call(envelope, "data")) {
        throw new ApiError("The server response did not include a data field.", {
            status: response.status,
            code: "MISSING_DATA",
        });
    }

    return envelope.data;
}

export function createApiClient({ baseUrl, fetchImpl = globalThis.fetch, onUnauthorized = () => {} }) {
    const normalizedBaseUrl = baseUrl.replace(/\/+$/, "");

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

        if (response.status === 401 && path !== "/api/auth/login") {
            onUnauthorized();
        }

        return readEnvelope(response, `${method} ${path}`);
    }

    return { request };
}
