import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = async (path) => readFile(new URL(path, import.meta.url), "utf8");

const labelPairs = [
    ["../src/components/BooksInventory.tsx", [
        "inventory-search", "inventory-sku", "inventory-book-name", "inventory-author",
        "inventory-price", "inventory-quantity", "inventory-min-stock",
    ]],
    ["../src/components/SalesPOS.tsx", ["sales-customer-name", "sales-book", "sales-quantity"]],
    ["../src/components/SchoolOrders.tsx", [
        "new-school-name", "school-contact", "school-phone", "school-address",
        "school-order-school", "school-order-product", "school-order-quantity", "school-order-search",
    ]],
    ["../src/components/Suppliers.tsx", ["supplier-name", "supplier-phone", "supplier-address", "suppliers-search"]],
    ["../src/components/PurchaseOrders.tsx", ["purchase-supplier", "purchase-search"]],
    ["../src/components/Reports.tsx", [
        "reports-start-date", "reports-end-date", "reports-search", "reports-search-inventory",
    ]],
    ["../src/components/Login.tsx", ["login-username", "login-password"]],
    ["../src/components/AdminSetup.tsx", [
        "setup-full-name", "setup-username", "setup-password", "setup-confirm-password",
    ]],
];

test("visible form controls have explicit, matching label associations", async () => {
    for (const [path, ids] of labelPairs) {
        const content = await source(path);
        for (const id of ids) {
            assert.ok(content.includes('id="' + id + '"'), path + " is missing control id " + id);
            assert.ok(content.includes('htmlFor="' + id + '"'), path + " is missing label association for " + id);
        }
    }
});

test("repeated order controls use matching dynamic labels and unique row IDs", async () => {
    const schoolOrders = await source("../src/components/SchoolOrders.tsx");
    assert.ok(schoolOrders.includes('htmlFor={\`order-status-\${order.id}\`}'));
    assert.ok(schoolOrders.includes('id={\`order-status-\${order.id}\`}'));

    const purchaseOrders = await source("../src/components/PurchaseOrders.tsx");
    for (const field of ["product", "quantity", "cost"]) {
        assert.ok(purchaseOrders.includes('htmlFor={\`purchase-' + field + '-\${index}\`}'));
        assert.ok(purchaseOrders.includes('id={\`purchase-' + field + '-\${index}\`}'));
    }
});

test("dashboard provides a skip link, navigation state, and visible keyboard focus", async () => {
    const page = await source("../src/app/page.tsx");
    const styles = await source("../src/app/globals.css");
    assert.ok(page.includes('href="#main-content"'));
    assert.ok(page.includes('id="main-content"'));
    assert.ok(page.includes('aria-current={activePage === item.name ? "page" : undefined}'));
    assert.ok(page.includes("pageHeadingRef.current?.focus()"));
    assert.ok(styles.includes(":focus-visible"));
});

test("module error recovery actions are keyboard-operable buttons", async () => {
    const boundary = await source("../src/components/ModuleErrorBoundary.tsx");
    assert.ok(boundary.includes('type="button"'));
    assert.ok(boundary.includes("onClick={this.reset}"));
    assert.ok(boundary.includes("onClick={this.props.onReturnToDashboard}"));
});

test("route-level error boundary exposes retry without leaking raw error messages", async () => {
    const boundary = await source("../src/app/error.tsx");
    assert.ok(boundary.includes('role="alert"'));
    assert.ok(boundary.includes("onClick={reset}"));
    assert.ok(boundary.includes("error.digest"));
    assert.equal(boundary.includes("{error.message}"), false);
});
