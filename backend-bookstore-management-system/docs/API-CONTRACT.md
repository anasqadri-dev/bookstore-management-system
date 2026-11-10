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
