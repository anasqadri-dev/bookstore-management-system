export function isBackendReady(data) {
    return Boolean(
        data
        && typeof data === "object"
        && data.status === "UP"
        && data.database === "UP"
    );
}
