# Bookstore Management System — Frontend

The bookstore dashboard is a **Next.js 16 App Router** application using React 19, TypeScript, Tailwind CSS 4, and Lucide icons. It connects to the local Spring Boot API; it is not a standalone database or authentication service.

## Prerequisites

- Node.js **22** and npm (the frontend CI uses Node 22).
- For login and authenticated API requests: **JDK 25** and the Spring Boot backend running locally.
- For a fresh database, the owner creates the first Admin through the one-time setup screen. Existing installations continue to use their existing accounts. Do not commit credentials or bootstrap passwords.

## Start locally

From the repository root, in one terminal:

```powershell
cd backend-bookstore-management-system
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open http://127.0.0.1:3000. The backend API listens on http://127.0.0.1:8080 by default. On a fresh database, the dashboard automatically displays the first-run Admin setup form. After the owner creates the account, the normal sign-in screen appears. See [the backend first-run setup instructions](../backend-bookstore-management-system/README.md#initial-admin-account-provisioning).

If the backend is not running or no account has been provisioned, login and session restoration will not work. Do not hard-code credentials or bypass the session check to test authenticated screens.

## API configuration and sessions

The API client is in [`src/lib/api.ts`](src/lib/api.ts). By default it calls `http://127.0.0.1:8080`. Open the frontend at `http://127.0.0.1:3000` too; using the same loopback host keeps the session cookie same-site. To override the address, create a local `frontend/.env.local` file (do not commit it):

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:8080
```

Restart the Next.js development server after changing the environment variable. Because this variable has the `NEXT_PUBLIC_` prefix, its value is exposed to the browser; **never put secrets in it**.

The client sends `credentials: "include"` so browser session cookies accompany API requests. Mutating requests obtain a CSRF token via `/api/auth/csrf` (except the login request). Backend cookie, CORS, authentication, and authorization rules still apply; a running server does not guarantee an authenticated session.

The source of truth for endpoint shapes and response envelopes is the [backend API contract](../backend-bookstore-management-system/docs/API-CONTRACT.md).

## Commands

Run these from `frontend/`:

| Command | Purpose |
| --- | --- |
| `npm ci` | Install exactly the dependencies in `package-lock.json` |
| `npm run dev` | Start development server on port 3000 |
| `npm run lint` | Run ESLint |
| `npm test` | Run deterministic frontend tests using Node.js built-in test runner |
| `npm run build` | Build the production Next.js application (standalone output) |
| `npm run build:desktop` | Build standalone output and copy static assets required by the Electron runtime |
| `npm run start` | Serve an existing production build |

**Automated frontend tests:** `npm test` runs the deterministic Node test suite in non-interactive mode. Current tests cover API envelope/session/CSRF behavior, role-policy and account-form helpers, navigation state, and backend health-response validation. This is a growing regression suite, not a substitute for the remaining React component-level workflow tests or manual Windows usability/accessibility checks.

The [frontend CI workflow](../.github/workflows/frontend-ci.yml) runs `npm ci`, `npm test`, `npm run lint`, and `npm run build` on Node 22 for frontend-related PRs. The [Windows desktop workflow](../.github/workflows/desktop-windows.yml) runs `npm run build:desktop` before packaging the Electron runtime.

## Project layout

- `src/app/page.tsx` — authenticated dashboard shell, sidebar navigation, module selection, and session restoration.
- `src/app/layout.tsx` and `src/app/globals.css` — application layout and shared styles.
- `src/components/Login.tsx` — login form.
- `src/components/AdminSetup.tsx` — one-time first-run Admin provisioning form.
- `src/components/BooksInventory.tsx` — inventory UI.
- `src/components/SalesPOS.tsx` and `SalesHistory.tsx` — point-of-sale and sales history screens.
- `src/components/Suppliers.tsx` and `PurchaseOrders.tsx` — supplier and purchasing screens.
- `src/components/SchoolOrders.tsx` — school order UI.
- `src/components/Reports.tsx` and `Settings.tsx` — reporting and settings screens.
- `src/lib/api.ts` — typed API request wrapper, cookie handling, and CSRF support.

## Current limitations and troubleshooting

- **Backend integration is incomplete.** Several management screens still use in-memory React state; records may disappear on navigation or refresh and are not yet persisted to SQLite. Do not rely on them for real bookstore transactions.
- **Login fails:** Confirm the backend is running, that an account exists, and inspect the browser Network tab for the failing request. A response of `401` means the request was not authenticated; it does not necessarily mean the database is disconnected.
- **Network/CORS errors:** Check the configured API URL, backend status, and browser console. Cookies and cross-origin settings must be compatible.
- **UI changes do not appear:** Ensure you are editing the correct branch, the frontend dev server is running, and the browser has refreshed.
- **Before requesting a merge:** Run lint and build, manually exercise affected flows, and verify the PR checks. Changes to backend integrations also need end-to-end verification.

For project-wide context, see the [repository README](../README.md).


## Validation commands

- `npm test` runs the Node built-in contract/model/accessibility suite.
- `npm run test:ui` runs Vitest component and session integration tests.
- `npm run lint` runs ESLint.
- `npm run build` creates the normal Next.js production build.
- `npm run build:desktop` creates the Next.js standalone output consumed by the Electron Windows package.
