# Performance and First-Time Clerk Acceptance Kit

This document provides repeatable procedures for SRS NFR-01 and NFR-13/NFR-14. It is a test protocol, not a test result. Do not mark either acceptance issue complete until the result tables are filled from an actual target-machine run.

## 1. Performance acceptance — issue #296

### Target environment

Run on the minimum supported Windows machine, not a GitHub-hosted CI runner. Record:

| Field | Actual value |
| --- | --- |
| Windows edition, version and build | |
| CPU model and logical cores | |
| Installed RAM | |
| Storage type and free space | |
| Display resolution and scaling | |
| Installer version and SHA-256 | |
| Application version / commit | |
| Database location and size | |
| Test date and tester | |

Use the packaged desktop app with internet disconnected. Use a clean, non-production database. Seed and record three dataset sizes (small, representative, and large); at minimum the representative dataset should contain 200 products and 200 sales. Keep the dataset identical across repeated runs.

### Measurement rules

- Measure from the user action/navigation start until the screen's primary content is usable, not merely until the request is sent.
- For API timing, measure from request start until the complete response body is received.
- Warm the application once before recording. Do not include first-time JVM startup in an individual endpoint sample; record cold-start time separately.
- Collect at least 20 samples per endpoint/action at each dataset size.
- Report median (p50), p95, minimum and maximum in milliseconds. Do not report only the fastest run.
- Use the local loopback backend; no external internet or cloud services are part of the measurement.
- Record failures, timeouts and loading-state duration. Do not discard slow samples without documenting the reason.

### Required API paths

Use an authenticated Admin session for Admin-only endpoints and a Staff session for shared read paths. Confirm the endpoint response is successful and its payload contains the expected data before recording latency.

| Area | Request |
| --- | --- |
| Product listing | `GET /api/products` |
| Product search | `GET /api/products?search=<known-title-or-SKU>` |
| Low-stock products | `GET /api/products/low-stock` |
| Dashboard aggregation | `GET /api/dashboard` |
| School list | `GET /api/schools` |
| Supplier list | `GET /api/suppliers` |
| Purchase-order list | `GET /api/purchase-orders` |
| Sales report | `GET /api/reports/sales?startDate=<YYYY-MM-DD>&endDate=<YYYY-MM-DD>` |
| Inventory report | `GET /api/reports/inventory` |

### Required frontend actions

Measure these from the renderer using the same target machine and dataset:

1. Sign in and open Dashboard.
2. Navigate Dashboard → Inventory and wait for the product list.
3. Search by a known SKU and then clear the search.
4. Enable the low-stock filter.
5. Open School Orders and wait for the order list.
6. Open Suppliers and wait for the supplier list.
7. Open Purchase Orders and wait for the order list.
8. Open Reports, generate a sales report, then an inventory report.

### Results

Record a separate row for every endpoint/action and dataset size.

| Dataset size | Endpoint/action | Samples | p50 (ms) | p95 (ms) | Min (ms) | Max (ms) | Pass p95 ≤ 3000 ms? |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| | | | | | | | |
| | | | | | | | |
| | | | | | | | |

Attach raw samples (CSV or JSON), dataset seed instructions, machine details, and screenshots of any slow/loading states. If p95 exceeds 3 seconds, record the slow path and open a focused optimization issue before closing #296.

## 2. First-time clerk usability — issue #297

### Participant and environment

- Recruit at least one representative first-time Staff user with basic computer skills who has not previously used this application.
- Use a clean test database and a non-production Staff account.
- Do not collect the participant's name, email, phone number, or other identifying details in the report. Use a neutral participant code such as `P-01`.
- The facilitator may read the task script but must not demonstrate the interface, suggest buttons, or correct the participant's navigation.
- Tell the participant that the application, not the participant, is being evaluated. Obtain consent before recording any screen or audio.

### Moderator-free task script

Give the participant only the following task:

> You are working at the bookstore counter. Sign in using the Staff account provided. Find the book titled **Acceptance Test Book** (SKU **AT-001**), check its available stock, add one copy to the cart, complete the cash sale, review the saved receipt, and then sign out. If you cannot continue, explain what you expected to happen.

Do not explain where controls are located or what order of buttons to press. Do not use real customer information or real payment details.

### Observer recording

| Measure | Result |
| --- | --- |
| Participant code | |
| Test date and application commit | |
| Task completed without assistance? | |
| Total completion time | |
| Incorrect actions / recoveries | |
| Help requests (count and exact task step) | |
| Product search and stock visibility understandable? | |
| Cart quantity and total understandable? | |
| Checkout and cash confirmation understandable? | |
| Receipt details understandable? | |
| Logout found and completed? | |
| Error messages / confusing labels | |
| Accessibility or keyboard blockers | |
| Defects filed and linked | |

### Outcome

- **Pass:** participant completes all steps without developer assistance, and no critical usability blocker prevents the workflow.
- **Conditional:** participant completes the workflow but needs assistance or encounters a recoverable confusing step. Record the exact blocker and create linked follow-up issues.
- **Fail:** participant cannot complete the workflow or a critical blocker prevents checkout, receipt review, or logout.

Attach the anonymized completed observation sheet. Do not invent a participant result or mark this issue complete based only on automated tests.

## 3. Traceability

- Performance results map to SRS NFR-01 (three-second response target).
- The clerk task maps to FR-01, FR-07, FR-08, FR-10 to FR-14 and NFR-13/NFR-14, including traceability case TC-49.
- Automated CI test timings are useful regression signals, but they do not replace the target Windows machine measurements or the observed first-time user session.
