import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const read = (path) => readFile(new URL(path, import.meta.url), "utf8");

const contracts = [
  {
    name: "Inventory and POS",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/product/controller/ProductController.java",
    frontend: ["../src/components/BooksInventory.tsx", "../src/components/SalesPOS.tsx"],
    backend: ["@RequestMapping(\"/api/products\")", "@GetMapping(\"/low-stock\")", "@PostMapping", "@PutMapping(\"/{id}\")", "@DeleteMapping(\"/{id}\")"],
    frontendPaths: ["/api/products", "/api/products/low-stock"],
  },
  {
    name: "POS and saved receipt retrieval",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/sale/controller/SaleController.java",
    frontend: ["../src/components/SalesPOS.tsx", "../src/components/SalesHistory.tsx"],
    backend: ["@RequestMapping(\"/api/sales\")", "@PostMapping", "@GetMapping", "@GetMapping(\"/{saleId}/receipt\")"],
    frontendPaths: ["/api/sales", "/api/sales/"],
  },
  {
    name: "School Orders and school directory",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/schoolorder/controller/SchoolOrderController.java",
    frontend: ["../src/components/SchoolOrders.tsx"],
    backend: ["@RequestMapping(\"/api/school-orders\")", "@GetMapping", "@PostMapping", "@PatchMapping(\"/{id}/status\")", "@GetMapping(\"/{id}/quotation\")"],
    frontendPaths: ["/api/school-orders", "/status", "/quotation", "/api/schools"],
  },
  {
    name: "School registration",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/school/controller/SchoolController.java",
    frontend: ["../src/components/SchoolOrders.tsx"],
    backend: ["@RequestMapping(\"/api/schools\")", "@GetMapping", "@PostMapping"],
    frontendPaths: ["/api/schools"],
  },
  {
    name: "Suppliers",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/supplier/controller/SupplierController.java",
    frontend: ["../src/components/Suppliers.tsx", "../src/components/PurchaseOrders.tsx"],
    backend: ["@RequestMapping(\"/api/suppliers\")", "@GetMapping", "@PostMapping"],
    frontendPaths: ["/api/suppliers"],
  },
  {
    name: "Purchase Orders",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/purchase/controller/PurchaseOrderController.java",
    frontend: ["../src/components/PurchaseOrders.tsx"],
    backend: ["@RequestMapping(\"/api/purchase-orders\")", "@GetMapping", "@PostMapping", "@PostMapping(\"/{id}/receive\")"],
    frontendPaths: ["/api/purchase-orders", "/receive"],
  },
  {
    name: "Sales and inventory reports",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/report/controller/ReportsController.java",
    frontend: ["../src/components/Reports.tsx", "../src/lib/reports-model.mjs"],
    backend: ["@RequestMapping(\"/api/reports\")", "@GetMapping(\"/sales\")", "@GetMapping(\"/inventory\")"],
    frontendPaths: ["/api/reports/sales", "/api/reports/inventory"],
  },
  {
    name: "Staff settings",
    controller: "../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/user/controller/UserController.java",
    frontend: ["../src/components/Settings.tsx"],
    backend: ["@RequestMapping(\"/api/users\")", "@GetMapping", "@PostMapping", "@PutMapping(\"/{id}\")", "@DeleteMapping(\"/{id}\")"],
    frontendPaths: ["/api/users"],
  },
];

for (const contract of contracts) {
  test(`frontend/backend contract: ${contract.name}`, async () => {
    const backend = await read(contract.controller);
    const frontends = await Promise.all(contract.frontend.map(read));
    for (const mapping of contract.backend) {
      assert.ok(backend.includes(mapping), `${contract.name}: backend mapping missing ${mapping}`);
    }
    for (const path of contract.frontendPaths) {
      assert.ok(frontends.some((source) => source.includes(path)),
        `${contract.name}: frontend request path missing ${path}`);
    }
  });
}

test("authentication, dashboard, and health contracts stay on the local API routes", async () => {
  const auth = await read("../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/user/controller/AuthController.java");
  const dashboard = await read("../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/dashboard/controller/DashboardController.java");
  const health = await read("../../backend-bookstore-management-system/src/main/java/com/power_puff/bms/health/controller/HealthController.java");
  const login = await read("../src/components/Login.tsx");
  const page = await read("../src/app/page.tsx");
  const readiness = await read("../src/components/BackendReadiness.tsx");

  for (const mapping of ["@RequestMapping(\"/api/auth\")", "@PostMapping(\"/login\")", "@GetMapping(\"/me\")", "@PostMapping(\"/logout\")", "@GetMapping(\"/csrf\")"]) {
    assert.ok(auth.includes(mapping), `AuthController is missing ${mapping}`);
  }
  assert.ok(dashboard.includes('@RequestMapping("/api/dashboard")'));
  assert.ok(health.includes('@RequestMapping("/api/health")'));
  assert.ok(login.includes("/api/auth/login"));
  assert.ok(page.includes("/api/auth/me"));
  assert.ok(page.includes("/api/auth/logout"));
  assert.ok(readiness.includes("/api/health"));
});
