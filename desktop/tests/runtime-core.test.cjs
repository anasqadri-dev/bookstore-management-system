const test = require("node:test");
const assert = require("node:assert/strict");
const { EventEmitter } = require("node:events");
const {
    isBackendHealthReady,
    isTrustedNavigation,
    buildBackendEnvironment,
    waitForService,
    stopChildProcess,
} = require("../runtime-core.cjs");

test("backend readiness requires HTTP 200 and both backend and database UP", () => {
    assert.equal(isBackendHealthReady(200, {
        success: true, data: { status: "UP", database: "UP" },
    }), true);
    assert.equal(isBackendHealthReady(200, {
        success: true, data: { status: "UP", database: "DOWN" },
    }), false);
    assert.equal(isBackendHealthReady(503, {
        success: false, data: { status: "DOWN", database: "DOWN" },
    }), false);
    assert.equal(isBackendHealthReady(200, { success: true, data: null }), false);
});

test("renderer navigation is restricted to the exact local origin", () => {
    assert.equal(isTrustedNavigation("http://127.0.0.1:3000/dashboard", "http://127.0.0.1:3000"), true);
    assert.equal(isTrustedNavigation("http://localhost:3000/", "http://127.0.0.1:3000"), false);
    assert.equal(isTrustedNavigation("https://example.com", "http://127.0.0.1:3000"), false);
    assert.equal(isTrustedNavigation("not a URL", "http://127.0.0.1:3000"), false);
});

test("backend environment points SQLite to user data and allows only the local renderer", () => {
    const env = buildBackendEnvironment({ PATH: "safe-path" }, "C:/Users/Owner/AppData/Roaming/BMS/bookstore.db");
    assert.equal(env.PATH, "safe-path");
    assert.equal(env.BMS_DATABASE_PATH, "C:/Users/Owner/AppData/Roaming/BMS/bookstore.db");
    assert.equal(env.BMS_BOOTSTRAP_ADMIN_ENABLED, "false");
});

test("service readiness retries transient failures until the probe succeeds", async () => {
    let attempts = 0;
    const result = await waitForService({
        timeoutMs: 100,
        intervalMs: 1,
        probe: async () => ++attempts === 3,
        sleep: async () => {},
    });
    assert.equal(result, true);
    assert.equal(attempts, 3);
});

test("service readiness stops immediately when the managed process exits", async () => {
    await assert.rejects(waitForService({
        timeoutMs: 100,
        probe: async () => false,
        isAlive: () => false,
        sleep: async () => {},
    }), /exited before becoming ready/);
});

test("service readiness reports a timeout when the endpoint never becomes healthy", async () => {
    await assert.rejects(waitForService({
        timeoutMs: 1,
        intervalMs: 1,
        probe: async () => false,
        sleep: async () => {},
    }), /Timed out/);
});

test("normal child-process shutdown waits for its exit event", async () => {
    class Child extends EventEmitter {
        exitCode = null;
        kill() {
            setImmediate(() => {
                this.exitCode = 0;
                this.emit("exit", 0, null);
            });
            return true;
        }
    }
    const child = new Child();
    assert.equal(await stopChildProcess(child, 100), "stopped");
});

test("unresponsive child-process shutdown escalates after the timeout", async () => {
    class Child extends EventEmitter {
        exitCode = null;
        calls = 0;
        kill() {
            this.calls += 1;
            return true;
        }
    }
    const child = new Child();
    assert.equal(await stopChildProcess(child, 5), "forced");
    assert.equal(child.calls, 2);
});
