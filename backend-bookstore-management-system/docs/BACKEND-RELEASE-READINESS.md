# Backend Release-Readiness Review

**Review date:** 2026-11-12  
**Target branch:** `develop`  
**Reviewed commit:** `ef2fc9d5a4c0760eb445ff961ed0e7b2d649a161`  
**Decision:** Backend integration readiness — **PASS**. Full desktop-product release readiness — **CONDITIONAL / NOT YET VERIFIED**.

## 1. Scope

This review covers the Spring Boot backend, its SQLite persistence layer, database backup/restore behavior, backend API security, and the automated backend regression suite.

It does not approve the Next.js frontend, Electron startup/packaging, printer configuration, or full Windows offline acceptance. Those require separate UI, desktop, hardware, or manual integration evidence.

## 2. Automated regression evidence

The repository's Backend CI workflow executes the full Maven Wrapper command:

```bash
cd backend-bookstore-management-system
./mvnw -B clean test
```

The run after the final backend QA merge completed successfully on the reviewed `develop` commit:

- Workflow: Backend CI
- Run: [43171807012](https://github.com/anasqadri-dev/bookstore-management-system/actions/runs/43171807012)
- Commit: `ef2fc9d5a4c0760eb445ff961ed0e7b2d649a161`
- Java: 25 (Temurin)
- Tests: 197
- Failures: 0
- Errors: 0
- Skipped: 0
- Maven result: `BUILD SUCCESS`

The CI job uses the repository's Maven Wrapper and runs `clean test`, so the result is not relying on a developer's pre-existing Maven installation or a stale `target` test directory.

## 3. Backend readiness checklist

| Area | Evidence on `develop` | Result |
|---|---|---|
| Build and regression | Full Maven Java 25 CI; 197 tests, no failures/errors/skips | PASS |
| Authentication | Login, logout, session restoration endpoint, BCrypt password hashing | PASS |
| Authorization | Admin/Staff role restrictions tested through session and controller authorization tests | PASS for backend routes under test |
| Product and inventory | CRUD/search, SKU validation, low-stock boundaries, deletion dependencies, saved data | PASS |
| POS sales | Item aggregation, decimal totals, stock deduction, receipt payload, rollback behavior | PASS |
| Sales reliability | 200 saved test sales completed without error or stock mismatch | PASS |
| Sale performance | Confirm-to-receipt path tested against the 5-second NFR-02 threshold | PASS for backend path |
| Dashboard | Authenticated summary endpoint, today-sales aggregate, low-stock count, five recent sales | PASS |
| Reports | Date boundaries, saved totals, inventory valuation, complete row payloads | PASS |
| Report/search performance | Inventory search and sales/inventory report APIs timed against the 3-second target | PASS for tested backend endpoints; not every screen/button |
| School orders | Registration, order creation, stock checks, quotation, status workflow | PASS |
| Suppliers and purchasing | Supplier APIs, purchase order creation, goods received, stock update, duplicate receipt protection | PASS |
| User administration | Create/list/update/deactivate, hashed passwords, duplicate and invalid input, Staff/anonymous restrictions | PASS |
| Activity logging | Persisted login, sale, and product-delete audit rows | PASS |
| Database schema | All eleven SDS tables, expected columns, foreign keys, no FK violations | PASS |
| SQLite migration | Sales, sale-item, and activity-log foreign-key migration preserves legacy rows and is idempotent | PASS |
| SQLite file persistence | Sales and school orders readable through a fresh SQLite connection | PASS for file persistence |
| Connection-pool lifecycle | Rollback on connection return, shutdown, integrity check, reopen, post-shutdown write | PASS |
| Backup | Validated destination, online SQLite backup, integrity check, no-overwrite behavior | PASS |
| Restore | Schema/integrity/FK validation, safety copy, restore verification, rollback handling | PASS |
| API errors | Consistent response envelope, status mappings, sanitized unexpected failures | PASS |
| Offline backend path | Local SQLite product/sale/report/backup workflow exercised | PARTIAL — see Section 4 |

## 4. Conditions before full desktop release

The backend is ready to integrate into `develop`, but the complete desktop product should not be called release-ready until these separate acceptance items are completed:

1. **Physical offline acceptance (SRS NFR-09 / issue #169).** Disconnect the target Windows machine from Wi-Fi and Ethernet after installation; launch Electron; sign in; exercise the actual screens for inventory, POS, school orders, suppliers/purchase orders, reports, backup and restore; restart while still disconnected; and attach evidence. The backend offline integration test proves a local SQLite workflow, but it does not physically disable networking or launch Electron.
2. **Frontend/backend contract checks (issues #178–#180).** Verify the Inventory/POS, School Orders/Suppliers, and Reports/Settings screens against the current API payloads, role rules, messages, and error statuses.
3. **Electron startup lifecycle (issue #181).** Verify the packaged desktop shell starts the local backend before the UI sends requests, handles backend startup failure, and shuts down cleanly.
4. **Printer acceptance (SRS TC-38).** Test receipt and report output with the target local printer and Windows print configuration. Backend receipt/report payload tests do not prove physical print quality.
5. **UI-only test cases.** Delete-cancel behavior (TC-10), first-time-user usability (TC-49), and all-screen labels/messages (TC-50) require frontend/manual review. The backend matrix records these as UI or partial coverage.

The full backend-to-SRS mapping and these boundaries are recorded in [BACKEND-TEST-TRACEABILITY.md](BACKEND-TEST-TRACEABILITY.md). The backend offline evidence and manual disconnected-machine procedure are in [BACKEND-OFFLINE-VALIDATION.md](BACKEND-OFFLINE-VALIDATION.md).

## 5. Known operational considerations

- The SQLite datasource path is relative to the backend process working directory. The packaged Windows application must use the intended local data directory consistently.
- Backup and restore APIs are Admin-only. Restore replaces the current database with the selected snapshot after validation and creates a temporary safety copy first.
- A restore failure must be treated as an operational incident until the service confirms automatic rollback or the preserved recovery-copy path is used.
- Existing historical rows must remain compatible with the schema migrations. The migration tests verify representative legacy sales, items, and activity logs survive migration; deployments should retain a separate backup before upgrading a real store database.
- The measured 3-second and 5-second performance checks cover the tested backend endpoints/paths and CI fixture sizes. They are not a blanket guarantee for every possible data volume or every frontend interaction.

## 6. Final decision

**Backend status: PASS for continued integration on `develop`.**

The current backend builds and passes the complete clean Maven regression suite. Its primary business workflows, API access control, schema integrity, SQLite persistence, activity logging, reports, backup/restore, and the documented backend performance paths have automated evidence.

**Full application release status: CONDITIONAL.**

Do not interpret this backend review as approval of the complete Windows desktop release. Issues #169, #178, #179, #180, and #181 and the UI/printer checks in Section 4 remain outside the verified backend scope. Keep those items open until their own evidence is reviewed.
