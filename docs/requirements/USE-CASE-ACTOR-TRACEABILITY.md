# SRS actor-to-use-case review matrix

**Status:** review supplement for issue #293. This file makes explicit the actor boundary and requirement assignments established by the issue evidence. It is not a replacement for the canonical SRS PDF diagram; the original PDF's complete UC-01–UC-18 list must be checked row-by-row before the diagram is regenerated.

## Actor boundary

| Actor | Boundary | Association rule |
| --- | --- | --- |
| Admin | Authenticated application user | May perform the Admin-only product catalogue operations and other Admin-restricted functions defined by the SRS. |
| Staff | Authenticated application user | May perform permitted counter and operational workflows; must not be associated with Admin-only functions. |
| School | External, non-authenticated party | Receives quotations and school-order outputs; does not sign in to BMS. |
| Supplier | External, non-authenticated party | Receives purchase-order outputs; does not sign in to BMS. |
| Walk-in Customer | External, non-authenticated party | Receives the sale receipt; does not sign in to BMS. |

## Explicit requirement restrictions

| SRS reference | Use case/function | Permitted actor | Diagram constraint |
| --- | --- | --- | --- |
| FR-04 | Add Product | Admin only | Connect only Admin. |
| FR-05 | Update Product | Admin only | Connect only Admin. |
| FR-06 | Delete Product | Admin only | Connect only Admin. |
| FR-02 | Authentication / role access | Admin and Staff, subject to role | Do not connect external parties to login. |
| FR-15–FR-27 | Remaining operational/reporting functions | Follow the role assignment in the SRS UC list and FR table | Do not infer a role from screen visibility; verify each UC against the source table before drawing. |

## Diagram review checklist

- [ ] Every UC-01–UC-18 appears exactly once and retains its source label.
- [ ] Every actor association matches both the UC list and the FR table.
- [ ] Add Product, Update Product, and Delete Product connect only to Admin.
- [ ] Staff has no association to supplier administration, purchasing, reporting, backup, or user administration where those are Admin-only in the SRS.
- [ ] School, Supplier, and Walk-in Customer remain outside the authentication boundary.
- [ ] Association lines are routed so crossing/overlap cannot imply an unintended association.
- [ ] A second reviewer independently checks all UC IDs and actor associations.

## Source limitation

The issue evidence identifies the conflict (page 8 diagram versus page 7 UC list and page 4 FR-04–FR-06), but the complete source PDF page content was not available in machine-readable form during this update. Therefore this supplement intentionally does not invent the missing UC-01–UC-18 names or role mappings. The canonical diagram must be corrected from the complete source table, then the corrected diagram must be inserted into the canonical SRS PDF.
