import test from "node:test";
import assert from "node:assert/strict";
import { rememberVisitedPage } from "../src/lib/navigation-state.mjs";

test("adds a visited module without discarding previously visited modules", () => {
    const previous = new Set(["Dashboard", "Books Inventory"]);
    const next = rememberVisitedPage(previous, "Sales / POS");
    assert.deepEqual([...next], ["Dashboard", "Books Inventory", "Sales / POS"]);
    assert.deepEqual([...previous], ["Dashboard", "Books Inventory"]);
});

test("revisiting a module preserves the same visited set", () => {
    const previous = new Set(["Dashboard", "Reports"]);
    assert.equal(rememberVisitedPage(previous, "Reports"), previous);
});
