# Backend — Bookstore Management System

## Architecture

The backend is a local Spring Boot REST API for the single-store, offline desktop application. It uses Java 25, Spring Boot 4.1.2, Spring Data JPA, Hibernate, Spring Security, and SQLite.

The code follows the SDS four-layer design:

- **Controller:** receives HTTP requests and returns API responses.
- **Service:** enforces business rules and coordinates transactions.
- **Repository:** reads and writes persisted entities.
- **Entity:** maps a database table to a Java class.

DTOs form the HTTP boundary; mapper classes translate between DTOs and entities. Controllers must not contain persistence or business rules.

## Package conventions

Each business module uses the following package layout where applicable:

`module/controller`, `module/dto`, `module/entity`, `module/mapper`, `module/repository`, and `module/service`.

Shared API envelopes and exception handling live under `common/response` and `common/exception`.

## Requirements

- JDK 25
- No separately installed Maven is required; use the Maven Wrapper.
- SQLite is stored in the current Windows user's persistent application-data directory (`%APPDATA%/BookstoreManagementSystem/bookstore.db` by default). Set `BMS_DATABASE_PATH` to override it. The database is outside the installation directory and does not depend on the process working directory.
- No internet connection is required at runtime.

## Run the backend (Windows PowerShell)

From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on port 8080 by default.

## Run tests (Windows PowerShell)

```powershell
.\mvnw.cmd clean test
```

Tests use an isolated SQLite test configuration. They must not write to the shop's `data/bookstore.db` file.

The SQLite file-persistence test uses a separate database under `target/` and verifies a committed row through a fresh SQLite connection. For a manual full application restart check, start the backend, create a product through `POST /api/products`, stop the backend normally, start it again, and retrieve the same product through `GET /api/products/{id}`. The production datasource is resolved to one absolute path before the connection pool is initialized.

## Initial Admin account provisioning

Admin bootstrap is disabled by default. For first-time setup only, set these environment variables in the same PowerShell session before starting the backend:

```powershell
$env:BMS_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BMS_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BMS_BOOTSTRAP_ADMIN_PASSWORD = "replace-with-a-strong-local-password"
$env:BMS_BOOTSTRAP_ADMIN_FULL_NAME = "Store Administrator"
.\mvnw.cmd spring-boot:run
```

The bootstrap creates an Admin only when the users table is empty. The password is BCrypt-hashed before persistence and is never logged. After the first successful startup, remove the bootstrap environment variables or set `BMS_BOOTSTRAP_ADMIN_ENABLED` to `false`. Existing user databases are never overwritten.

## API conventions

Successful and failed controller responses use `ApiResponse<T>` with `success`, `message`, and `data` fields. Validation errors return HTTP 400, missing resources 404, data conflicts 409, unauthenticated requests 401, and forbidden requests 403.

Login creates a server-side session. The desktop frontend must retain the session cookie and send it on subsequent requests. Logout invalidates that session.

All request DTOs validate untrusted input at the controller boundary with Jakarta Bean Validation. Services enforce business invariants again where those invariants protect data integrity.

## Quality rules

- Keep commits focused and small.
- Add automated tests for new business behavior and boundary conditions.
- Use transactions for multi-row business operations.
- Never persist or log plain-text passwords.
- Do not add fields or workflows absent from the approved SRS/SDS without documenting an agreed scope change.


## SQLite schema and versioned migrations

The initial 11-table schema is defined in `src/main/resources/schema.sql`. Spring SQL initialization executes this idempotent baseline script before Hibernate starts. Existing tables are not recreated or overwritten by the script.

Production uses `spring.jpa.hibernate.ddl-auto=validate`. Hibernate checks that mapped entities match the database; it does not create, alter, or update production tables automatically.

All schema changes after the baseline must be implemented as a `VersionedSqliteMigration`:

1. Add a Spring component implementing `VersionedSqliteMigration`.
2. Assign the next unique sequential version number. Do not reuse an applied version.
3. Provide a clear description and a stable `checksumSource()` describing the migration's effective SQL/data transformation.
4. Implement `migrate(Connection)`. Use the supplied connection; do not create another connection, commit, or toggle auto-commit inside a migration.
5. Preserve existing rows, validate the resulting schema, and fail with an actionable exception if the migration cannot be completed.
6. Add tests for a fresh database, upgrade from the previous schema, repeat startup, row/data preservation, and failure rollback.

`VersionedSqliteMigrationRunner` executes migrations in ascending version order and records the applied version, description, SHA-256 checksum, and application timestamp in the internal `schema_migrations` table. The schema change and its history row are committed in the same SQLite transaction. A migration already recorded with a different description or checksum causes startup to fail; never edit an applied migration—add a new version instead.

For SQLite table rebuilds, the runner disables foreign-key enforcement only on its dedicated migration connection before beginning the migration transaction. It restores foreign-key enforcement after every migration and runs `PRAGMA foreign_key_check` before application startup completes. Business connections continue to use the normal foreign-key setting.

There is no automatic down-migration for table rebuilds. Before distributing an application release that includes a schema migration, take a verified SQLite backup. Recovery from an unsuccessful destructive change uses the verified pre-upgrade backup and the documented restore procedure. A migration failure rolls back its own transaction and prevents the backend from starting against a partially applied schema.
