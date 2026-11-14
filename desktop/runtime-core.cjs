const { setTimeout: delay } = require("node:timers/promises");

function isBackendHealthReady(statusCode, payload) {
    return statusCode === 200
        && payload !== null
        && typeof payload === "object"
        && payload.success === true
        && payload.data?.status === "UP"
        && payload.data?.database === "UP";
}

function isTrustedNavigation(target, localOrigin) {
    try {
        return new URL(target).origin === new URL(localOrigin).origin;
    } catch {
        return false;
    }
}

function buildBackendEnvironment(baseEnvironment, databasePath) {
    return {
        ...baseEnvironment,
        BMS_DATABASE_PATH: databasePath,
        BMS_BOOTSTRAP_ADMIN_ENABLED: "false",
    };
}

async function waitForService({
    probe,
    timeoutMs = 45000,
    intervalMs = 250,
    isAlive = () => true,
    sleep = delay,
}) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (!isAlive()) {
            throw new Error("The managed service exited before becoming ready.");
        }
        try {
            if (await probe()) return true;
        } catch {
            // Startup may temporarily refuse connections; retry until the deadline.
        }
        const remaining = deadline - Date.now();
        if (remaining > 0) await sleep(Math.min(intervalMs, remaining));
    }
    throw new Error("Timed out waiting for the managed service to become ready.");
}

function stopChildProcess(child, timeoutMs = 5000) {
    if (!child || child.exitCode !== null) return Promise.resolve("already-stopped");
    return new Promise((resolve, reject) => {
        let settled = false;
        let timer;
        const finish = (result) => {
            if (settled) return;
            settled = true;
            if (timer) clearTimeout(timer);
            child.removeListener("exit", onExit);
            child.removeListener("error", onError);
            resolve(result);
        };
        const onExit = () => finish("stopped");
        const onError = (error) => {
            if (settled) return;
            settled = true;
            if (timer) clearTimeout(timer);
            child.removeListener("exit", onExit);
            child.removeListener("error", onError);
            reject(error);
        };
        child.once("exit", onExit);
        child.once("error", onError);
        try {
            child.kill();
        } catch (error) {
            onError(error);
            return;
        }
        timer = setTimeout(() => {
            try {
                child.kill();
                finish("forced");
            } catch (error) {
                onError(error);
            }
        }, timeoutMs);
    });
}

module.exports = {
    isBackendHealthReady,
    isTrustedNavigation,
    buildBackendEnvironment,
    waitForService,
    stopChildProcess,
};
