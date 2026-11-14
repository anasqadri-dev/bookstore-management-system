# Backend API Contract

## General

Base URL: `http://127.0.0.1:8080`

JSON responses use the envelope:

```json
{
  "success": true,
  "message": "Operation completed",
  "data": {}
}
```

Error responses use the same envelope with `success: false`. Validation errors return a field-to-message map in `data`.

Login uses an HTTP session. Desktop UI requests after login must retain and send the `JSESSIONID` cookie. No cloud identity service is required.

## Health and authentication

- `GET /api/health` — public local backend health check.
- `POST /api/auth/login` — accepts `username` and `password`; creates an authenticated session.
- `GET /api/auth/me` — returns the authenticated account identity and role.
- `POST /api/auth/logout` — invalidates the authenticated session.

## First-run Admin setup

- `GET /api/setup/status` — public local status check; returns `data.setupRequired` as `true` only when no account exists and the one-time setup marker is absent.
- `POST /api/setup` — creates the first Admin from `username`, `password`, and `fullName`; requires the session-bound `X-CSRF-TOKEN` header and returns HTTP 201 on success.
- A completed setup or an existing user account causes later provisioning requests to return HTTP 409. Invalid or weak credentials return HTTP 400.
- The Admin row and singleton setup marker are committed atomically. Failed transactions roll back the marker and may be retried; concurrent requests cannot create multiple initial Admin accounts.

## Inventory

- `GET /api/products` — list products; optional `search` query parameter matches title or SKU.
- `GET /api/products/{id}` — get one product.
- `GET /api/products/low-stock` — list products where quantity is less than or equal to min_stock.
- `POST /api/products` — create product (Admin).
- `PUT /api/products/{id}` — update product (Admin).
- `DELETE /api/products/{id}` — delete an unused product (Admin).

Product fields follow the SDS data dictionary: `id`, `sku`, `title`, optional `author`, `price`, `quantity`, and `min_stock` (JSON field `minStock`). No category field is introduced because it is not in the SDS product data dictionary.

## Dashboard

Authenticated Admin and Staff users:

- `GET /api/dashboard` — return the dashboard summary.
- `totalBooks` is the number of product records.
- `todaySales` is the sum of saved sale totals from the start of the current shop-local day (inclusive) to the start of the next day (exclusive).
- `lowStock` is the number of products where quantity is less than or equal to minStock.
- `suppliers` is the number of registered supplier records.
- `recentSales` contains up to the five most recent sales, newest first, with sale date, customer, cashier, and total.

The endpoint requires authentication. Both Admin and Staff may access the dashboard; unauthenticated callers receive HTTP 401.

## Reports

Admin-only endpoints:

- `GET /api/reports/sales?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` — return sales within the inclusive date range, including total sale count, total amount, and individual sale rows.
- `GET /api/reports/inventory` — return product quantities, low-stock counts, stock valuation, and product rows.

Both endpoints require an authenticated Admin session. Staff and unauthenticated callers are denied.

For the sales report, both dates are required and must use ISO `YYYY-MM-DD` format. The start date must not be after the end date. Invalid or missing date parameters and reversed date ranges return HTTP 400 using the standard API error envelope.

## Sales / POS

Admin and Staff:

- `POST /api/sales` — confirm and save a cash sale; validates stock, calculates totals, deducts stock, and records the sale activity in one transaction.
- `GET /api/sales/{saleId}/receipt` — retrieve the saved receipt for a sale.

The create request accepts an optional `customerName` and a non-empty `items` array. Each item contains only `productId` and `quantity`; the backend reads the current product price and does not trust a price supplied by the desktop client.

Example request:

```json
{
  "customerName": "Walk-in Customer",
  "items": [
    { "productId": 7, "quantity": 2 },
    { "productId": 9, "quantity": 1 }
  ]
}
```

The receipt response contains `id`, `saleDate`, `customerName`, `totalAmount`, `cashierName`, and `items`. Each receipt item contains `productId`, `sku`, `title`, `quantity`, `unitPrice`, and `subtotal`. The saved `unitPrice` and `subtotal` preserve the amounts used for that sale when a product's current price changes later.

### POS error cases

| Situation | HTTP status | Response behavior |
|---|---:|---|
| Sale has no items or the items list is missing | 400 | Validation error; no sale is created. |
| An item is null, a product id is missing/non-positive, or a quantity is missing/non-positive | 400 | Validation error; no sale is created. |
| Combined quantity for a repeated product exceeds the supported integer range | 400 | The request is rejected; no stock is deducted. |
| A referenced product does not exist | 404 | The response identifies the missing product. |
| Requested quantity exceeds current available stock | 400 | The response reports the product, requested quantity, and available quantity; no item in that sale is deducted. |
| The requested receipt id does not exist | 404 | The response reports that the sale was not found. |
| The caller is not logged in | 401 | Authentication is required. |
| The caller is authenticated but is not Admin or Staff | 403 | The operation is forbidden. |

The checkout service validates every product and requested quantity before it changes stock. Sale rows, sale-item rows, stock deductions, and the SALE activity event are committed in one transaction. If a runtime failure occurs while saving the sale or its activity event, the transaction rolls back so the sale and stock deduction are not partially committed.

## Schools and School Orders

Admin-only endpoints:

- `POST /api/schools` — register a school using its name and optional contact person, phone, and address.
- `GET /api/schools` — list registered schools.
- `POST /api/school-orders` — create a school order for a registered school.
- `GET /api/school-orders` — list school orders.
- `GET /api/school-orders/{id}` — retrieve one school order.
- `PATCH /api/school-orders/{id}/status` — update the order status.
- `GET /api/school-orders/{id}/quotation` — generate a quotation from the saved order items.

A school order request contains a `schoolId` and a non-empty `items` array. Each item contains `productId` and `quantity`. New orders start with status `PENDING`. The supported status values are `PENDING`, `READY`, and `DELIVERED`.

The status update request contains a `status` field set to one of those three values.

### School-order error cases

| Situation | HTTP status | Response behavior |
|---|---:|---|
| The school id is missing or invalid, or the items list is empty | 400 | The order request is rejected. |
| An item has a missing/non-positive product id or quantity | 400 | The order request is rejected. |
| The combined quantity for a product repeated in the request exceeds available stock | 400 | The response reports requested and available quantities; no order is saved and stock remains unchanged. |
| A registered school cannot be found | 404 | The response identifies the missing school. |
| A referenced product cannot be found | 404 | The response identifies the missing product. |
| The requested order or quotation does not exist | 404 | The response reports that the school order was not found. |
| The caller is not an Admin | 403 | School and school-order endpoints are forbidden. |

School-order creation validates every requested product and the aggregated quantity for each product before saving the order. Creating a school order does not deduct inventory; the saved order remains pending until its status is updated.

Example request:

```json
{
  "schoolId": 3,
  "items": [
    { "productId": 7, "quantity": 2 },
    { "productId": 9, "quantity": 1 }
  ]
}
```

The quotation response includes the school order, school, date, status, line items, each line total, and the grand total. Each line total is calculated as the saved `unitPrice` multiplied by its quantity. The quotation uses the price snapshot stored when the order was created, so later product price changes do not alter earlier quotations.

## Suppliers

Admin-only endpoints:

- `POST /api/suppliers` — register a supplier using its required name and optional phone and address.
- `GET /api/suppliers` — list suppliers in name order.

The supplier data model follows the SDS: `id`, `name`, `phone`, and `address`. A supplier name is required; phone and address may be omitted.

Example request:

```json
{
  "name": "Regional Book Distributor",
  "phone": "03001234567",
  "address": "Main Market"
}
```

## Purchase Orders

Admin-only endpoints:

- `POST /api/purchase-orders` — create a purchase order for a registered supplier.
- `GET /api/purchase-orders` — list purchase orders.
- `GET /api/purchase-orders/{id}` — retrieve one purchase order.
- `POST /api/purchase-orders/{id}/receive` — record goods received for an `ORDERED` purchase order.

A purchase order request contains a `supplierId` and a non-empty `items` array. Each item contains `productId`, `quantity`, and `unitCost`. The unit cost is the supplier purchase cost and is stored separately from the product's retail price. New purchase orders start with status `ORDERED`; after goods receipt, the status is `RECEIVED` and `receivedDate` is recorded.

Example request:

```json
{
  "supplierId": 4,
  "items": [
    { "productId": 7, "quantity": 10, "unitCost": 55.00 },
    { "productId": 9, "quantity": 5, "unitCost": 72.50 }
  ]
}
```

The receive action marks the order `RECEIVED`, records `receivedDate`, and increases each product's stock by the quantities saved on the purchase-order items. The status/date update and all stock changes are committed in one transaction; if any stock update fails, the transaction rolls back. Receiving an order more than once returns HTTP 409 and does not increase stock again.

### Purchase-order error cases

| Situation | HTTP status | Response behavior |
|---|---:|---|
| Supplier id or item data is missing/invalid, including a non-positive quantity or unit cost | 400 | The request is rejected and no purchase order is saved. |
| A referenced supplier or product does not exist | 404 | The response identifies the missing resource and no purchase order is saved. |
| The purchase order does not exist | 404 | The response reports that the purchase order was not found. |
| Received quantities would exceed the supported stock range | 400 | The receipt is rejected; the order stays ORDERED and stock remains unchanged. |
| Goods have already been received for the order | 409 | The repeated receive request is rejected; stock remains unchanged. |
| The caller is not an Admin | 403 | Purchase-order endpoints are forbidden. |

## Database Backup and Restore

Admin-only endpoints:

- `POST /api/backup` — create a consistent SQLite database backup at the requested absolute file path.
- `POST /api/backup/restore` — validate and restore a selected SQLite backup into the active local database.

Create-backup request:

```json
{
  "destinationPath": "D:/BookstoreBackups/bookstore-2026-11-12.db"
}
```

Restore request:

```json
{
  "backupPath": "D:/BookstoreBackups/bookstore-2026-11-12.db"
}
```

The backup destination must use a supported SQLite extension (`.db`, `.sqlite`, or `.sqlite3`), must be an absolute path in an existing writable directory, and must not already exist or point to the active database file. The service verifies the created backup with SQLite `integrity_check`.

Restore checks that the selected file is readable, non-empty, has a valid SQLite integrity check, contains the eleven tables defined in the SDS, and has no foreign-key violations. Before replacing the active database, the backend creates a temporary safety copy. If the restore or its post-restore verification fails, the backend attempts to restore that safety copy. If automatic rollback cannot be verified, the recovery-copy path is returned in the server error for administrator recovery.

Unauthenticated callers receive HTTP 401. Staff users receive HTTP 403. Invalid paths, files, or request values are rejected without modifying the active database.

## User account administration

Admin-only:

- `GET /api/users` — list accounts without password hashes.
- `POST /api/users` — create a Staff account.
- `PUT /api/users/{id}` — update a Staff account's full name, optional password, and active flag.
- `DELETE /api/users/{id}` — deactivate a Staff account. The record is retained for historical/audit relationships.

## HTTP status conventions

- 200 — successful read or update.
- 201 — resource created.
- 400 — invalid request data.
- 401 — authentication is required or credentials are invalid.
- 403 — authenticated user lacks the required role.
- 404 — resource does not exist.
- 409 — duplicate or referential-integrity conflict.
- 500 — unexpected server error; internal details are not returned to clients.
