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

Error responses use the same envelope with `success: false` and a null `data` value, except validation errors which return a field-to-message map.

## Health

- `GET /api/health` — returns backend process health.

## Inventory

- `GET /api/products` — list products; optional `search` query parameter matches title or SKU.
- `GET /api/products/{id}` — get one product.
- `GET /api/products/low-stock` — list products where quantity is less than or equal to min_stock.
- `POST /api/products` — create product (Admin).
- `PUT /api/products/{id}` — update product (Admin).
- `DELETE /api/products/{id}` — delete an unused product (Admin).

Product request fields follow the SDS data dictionary: `sku`, `title`, optional `author`, `price`, `quantity`, and `minStock`.

## HTTP status conventions

- 200 — successful read or update.
- 201 — resource created.
- 204 is not used; delete returns the standard API response envelope.
- 400 — invalid request data.
- 401 — authentication is required or invalid.
- 403 — authenticated user lacks the required role.
- 404 — resource does not exist.
- 409 — duplicate or referential-integrity conflict.
- 500 — unexpected server error; internal details are not returned to clients.
