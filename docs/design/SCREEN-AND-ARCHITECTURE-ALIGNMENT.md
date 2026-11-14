# Delivered screen and architecture alignment

**Status:** implementation-alignment supplement for issue #300. This describes the delivered navigation and known runtime architecture at the repository release. It does not silently approve new SRS scope.

## Navigation-to-SDS screen map

The current frontend navigation contains these nine entries. The SDS screen inventory and diagrams should use the same names and boundaries, or explicitly document an approved scope decision.

| Delivered navigation entry | Responsibility to document | Alignment note |
| --- | --- | --- |
| Dashboard | Summary/overview | Main landing screen. |
| Inventory | Product catalogue, stock, search and low-stock view | Product CRUD permissions remain role-restricted by the SRS. |
| POS | Counter cart and cash-sale workflow | Receipt is based on the persisted sale response. |
| Suppliers | Supplier records and supplier-facing information | Keep distinct from purchase-order workflow if both are separate navigation entries. |
| School Orders | School quotation/order workflow | School remains an external party, not an authenticated BMS user. |
| Reports | Operational and sales reporting | Report access follows SRS role restrictions. |
| Settings | Application configuration | Document only settings actually present in the delivered UI. |
| Sales History | Persisted sales and historical receipt retrieval | This is a delivered navigation entry; record it as an implemented screen and trace it to the approved requirement/change decision rather than silently treating it as an original SRS requirement. |
| Purchase Orders | Purchase-order workflow | Separate delivered screen; document its boundary from Suppliers and goods-received behavior. |

## Runtime and component map

The delivered desktop runtime uses an Electron shell to coordinate the local frontend and Spring Boot backend. The packaged application bundles the frontend standalone runtime and a Java runtime/backend; the backend persists data locally in SQLite. The desktop process lifecycle and security helpers have automated tests, and the Windows installer is produced by the Windows Desktop CI workflow.

The backend architecture remains the documented four-layer design:

1. **Controller/API layer** — HTTP endpoints and request/response mapping.
2. **Service layer** — business rules and orchestration.
3. **Repository/data-access layer** — persistence operations.
4. **Model/entity layer** — domain and persistence representations.

The renderer communicates with the local backend through the shared API client. The runtime must bind backend services to loopback only; the packaged application must not depend on an external hosted API for core workflows.

## Canonical SDS update checklist

- [ ] Update screen inventory from seven screens to the nine delivered navigation entries above, with scope/change references for additions.
- [ ] Resolve the documented Suppliers versus Purchase Orders boundary.
- [ ] Document Sales History as a delivered capability and cite its approved scope/change record.
- [ ] Replace any statement that Electron is merely planned with the delivered Electron runtime architecture.
- [ ] Update component, deployment, process-lifecycle, and repository-structure diagrams to match the release commit.
- [ ] Show local frontend, Electron shell, Spring Boot backend, SQLite persistence, and loopback API communication.
- [ ] Preserve the four-layer backend architecture; do not mix architectural styles.
- [ ] Mark each component as implemented, partial, or planned based on repository evidence.
- [ ] Have a second contributor perform a source-tree-to-SDS walkthrough.
- [ ] Regenerate and replace the canonical SDS PDF after review.

## Source limitation

This supplement aligns to the delivered navigation/runtime and the discrepancy described in issue #300. The canonical SDS PDF is binary and was not available as readable source text in this workflow, so page-level edits and diagram replacement are not claimed. This file is a reviewable source supplement, not a substitute for the canonical PDF deliverable.
