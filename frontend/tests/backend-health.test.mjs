import test from "node:test";
import assert from "node:assert/strict";
import { isBackendReady } from "../src/lib/backend-health.mjs";

test("accepts only a healthy backend with a healthy database", () => {
    assert.equal(isBackendReady({ status: "UP", database: "UP" }), true);
});

test("rejects a backend whose database is unavailable", () => {
    assert.equal(isBackendReady({ status: "UP", database: "DOWN" }), false);
});

test("rejects a backend that reports itself unavailable", () => {
    assert.equal(isBackendReady({ status: "DOWN", database: "DOWN" }), false);
});

test("rejects missing or malformed health data", () => {
    for (const value of [null, undefined, {}, { status: "UP" }, { database: "UP" }, "UP"]) {
        assert.equal(isBackendReady(value), false);
    }
});
