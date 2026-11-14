import test from "node:test";
import assert from "node:assert/strict";
import { canAccessSection, canManageInventory, getVisibleSections } from "../src/lib/permissions.mjs";
import {
    buildCreateStaffPayload,
    buildUpdateStaffPayload,
    validateStaffDraft,
    validateStaffUpdate,
} from "../src/lib/user-admin-model.mjs";

test("Staff sees only the documented Staff sections", () => {
    const sections = ["Dashboard", "Books Inventory", "Sales / POS", "Sales History", "Suppliers", "Reports", "Settings"]
        .map((name) => ({ name }));
    assert.deepEqual(getVisibleSections("STAFF", sections).map((item) => item.name),
        ["Dashboard", "Books Inventory", "Sales / POS", "Sales History"]);
    assert.equal(canAccessSection("STAFF", "Reports"), false);
    assert.equal(canAccessSection("STAFF", "Sales / POS"), true);
});

test("Admin can access all sections and manage inventory", () => {
    assert.equal(canAccessSection("ADMIN", "Settings"), true);
    assert.equal(canManageInventory("ADMIN"), true);
    assert.equal(canManageInventory("STAFF"), false);
});

test("Staff creation validates UTF-8 password byte length and required fields", () => {
    assert.match(validateStaffDraft({ username: " ", fullName: "Staff", password: "StrongPass123" }), /Username/);
    assert.match(validateStaffDraft({ username: "staff", fullName: " ", password: "StrongPass123" }), /Full name/);
    assert.match(validateStaffDraft({ username: "staff", fullName: "Staff", password: "short" }), /8 to 72/);
    assert.match(validateStaffDraft({ username: "staff", fullName: "Staff", password: "é".repeat(37) }), /8 to 72/);
    assert.equal(validateStaffDraft({ username: " staff ", fullName: " Staff Name ", password: "StrongPass123" }), "");
});

test("staff payloads contain only supported account fields", () => {
    assert.deepEqual(buildCreateStaffPayload({
        username: " staff ", fullName: " Staff ", password: "StrongPass123",
    }), { username: "staff", fullName: "Staff", password: "StrongPass123" });
    assert.deepEqual(buildUpdateStaffPayload({
        fullName: " Updated ", newPassword: "", active: false,
    }), { fullName: "Updated", newPassword: null, active: false });
    assert.match(validateStaffUpdate({ fullName: "Staff", newPassword: "weak", active: true }), /8 to 72/);
});
