# Backend API Contract

## General

Base URL: `http://localhost:8080`

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

## Inventory

- `GET /api/products` — list products; optional `search` query parameter matches title or SKU.
- `GET /api/products/{id}` — get one product.
- `GET /api/products/low-stock` — list products where quantity is less than or equal to min_stock.
- `POST /api/products` — create product (Admin).
- `PUT /api/products/{id}` — update product (Admin).
- `DELETE /api/products/{id}` — delete an unused product (Admin).

Product fields follow the SDS data dictionary: `id`, `sku`, `title`, optional `author`, `price`, `quantity`, and `min_stock` (JSON field `minStock`). No category field is introduced because it is not in the SDS product data dictionary.

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
