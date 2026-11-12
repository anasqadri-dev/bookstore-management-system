# Backend Architecture Review

## 1. Review scope

This review checks the Spring Boot backend against the four-layer backend design described in the Software Design Specification (SDS), Section 1.2.

The SDS defines these backend layers:

1. Controller — receives screen/API requests and returns responses.
2. Service — owns business rules and application operations.
3. Repository — reads and writes persisted records.
4. Entity — maps Java objects to database tables.

This review is limited to the backend and its SQLite persistence integration. It does not assess the Next.js interface or Electron desktop shell.

## 2. Observed backend structure

The backend is organized primarily by business capability. Within each capability, the source tree separates the controller, service, repository, entity, DTO, and mapper responsibilities.

Examples from the current source:

| SDS layer | Implementation location | Example |
|---|---|---|
| Controller | `<capability>/controller` | `sale/controller/SaleController.java` |
| Service | `<capability>/service` | `sale/service/SaleService.java` |
| Repository | `<capability>/repository` | `sale/repository/SaleRepository.java` |
| Entity | `<capability>/entity` | `sale/entity/Sale.java` |

Other capabilities use the same package convention, including products, schools, school orders, suppliers, purchase orders, users, reports, and activity logging.

## 3. Layer-by-layer findings

### 3.1 Controller layer

Controllers expose REST routes, bind request parameters and bodies, apply request validation, enforce role requirements where appropriate, invoke services, and wrap results in the standard `ApiResponse` envelope.

Example: `SaleController` exposes `POST /api/sales` and `GET /api/sales/{saleId}/receipt`. It delegates sale creation and receipt retrieval to `SaleService` rather than implementing checkout calculations in the controller.

### 3.2 Service layer

Services contain business rules and coordinate operations across repositories and other services. Transaction boundaries are applied at the service layer for operations that must commit atomically.

Example: `SaleService` validates the request, aggregates repeated product quantities, loads products and the cashier, checks available stock, calculates totals, snapshots item prices, deducts stock, saves the sale, and records the activity event. The operation is annotated with `@Transactional`, so the sale, stock changes, and activity event share one transaction.

The backup and restore services also coordinate file validation, SQLite operations, integrity checks, and failure recovery. Those operations are kept out of controllers.

### 3.3 Repository layer

Repositories extend Spring Data JPA repository interfaces and expose persistence operations or narrowly scoped queries. Controllers do not need to know how a table is queried.

Example: `SaleRepository` extends `JpaRepository<Sale, Integer>` and declares the sales date-range and recent-sales queries used by reporting and the dashboard.

### 3.4 Entity layer

Entities define the persistent data model using JPA annotations and map to the SDS table names. Relationships are expressed through entity relationships and are enforced in SQLite by the startup migrations where the SQLite Hibernate dialect cannot add the constraints through schema update.

Example: `Sale` is annotated with `@Entity` and `@Table(name = "sales")`; it maps the sale date, customer, total, cashier relationship, and sale-item collection.

## 4. Supporting backend structures

The following packages support the four core layers; they do not introduce additional business layers:

- `dto`: API request and response shapes, separated from the JPA entities.
- `mapper`: maps entities to API response DTOs and back where required.
- `common/response`: the shared `ApiResponse` success/error envelope.
- `common/exception`: application exceptions and centralized REST error handling.
- `security`: authentication, session persistence, password encoding, and role enforcement.
- `database`, `purchase/config`, and `schoolorder/config`: SQLite startup migrations for relationships the dialect cannot add through ordinary schema update.
- `backup/service`: file validation, SQLite online backup/restore, integrity verification, and recovery handling.

## 5. Database and transaction alignment

The backend datasource is configured for a local SQLite file (`jdbc:sqlite:./data/bookstore.db`). Foreign-key enforcement is enabled on pooled connections.

The SDS data model contains eleven tables. The database contract tests verify the table names, columns, and documented foreign-key relationships. Explicit SQLite table-rebuild migrations are used for sales/activity logs, school orders, and purchase orders where required to enforce the documented relationships while preserving existing records.

Important persistence protections include:

- Sale checkout writes sale rows, sale items, stock changes, and the activity event within one transaction.
- School-order creation and purchase-order receipt are transactional.
- Backup creation uses SQLite's online backup capability and verifies the generated file.
- Restore validates the file, schema, and foreign keys before replacing the active database; it first creates a safety copy and verifies the restored database.

## 6. Review conclusion

The inspected backend structure is consistent with the SDS four-layer design. The code is organized by business capability, while preserving the Controller → Service → Repository → Entity dependency direction. DTOs, mappers, security, exception handling, and SQLite migrations act as supporting infrastructure around those layers.

No architectural rewrite is recommended as part of this review. Continuing to preserve the existing separation is the appropriate approach for future backend work.

## 7. Maintenance recommendations

1. Keep controllers focused on HTTP binding, validation, authorization declarations, and response mapping; do not add repository queries or business calculations to controllers.
2. Keep multi-step business operations and transaction boundaries in services.
3. Keep repository methods focused on persistence queries; place aggregation and business interpretation in services unless a measured query-performance issue justifies a database aggregate.
4. Keep JPA entities out of public API responses; expose DTOs and map them at the boundary.
5. Keep SQLite migrations idempotent, transactional where possible, and covered by tests that prove existing records survive migration.
6. Keep the API response envelope consistent across controller advice and the Spring Security entry/denial handlers.
7. Continue adding integration tests at the service/API boundary for role checks, persistence, rollback, and error responses.

## 8. Review limitations

This is a source-structure and representative-flow review, not a complete static architecture-enforcement report. It does not run ArchUnit or another dependency-rule tool over every class. It also does not assess frontend/Electron integration or packaging lifecycle behavior.
