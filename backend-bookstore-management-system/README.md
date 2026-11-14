# Backend — Bookstore Management System

## Architecture
The backend is a local Spring Boot REST API for the single-store, offline desktop application. It uses Java 25, Spring Boot 4.1.2, Spring Data JPA, Hibernate, Spring Security, and SQLite.

The code follows the SDS four-layer design:

- **Controller:** receives HTTP requests and returns API responses.
- **Service:** enforces business rules and coordinates transactions.
- **Repository:** reads and writes persisted entities.
- **Entity:** maps a database table to a database table.

DTOs form the HTTP boundary; mapper classes translate between DTOs and entities. Controllers must not contain persistence or business rules.

## Package conventions
Each business module uses the following package layout where applicable:

`module/controller`, `module/dto`, `module/entity`, `module/mapper`, `module/repository`, and `module/service`.

Shared API envelopes and exception handling live under `common/response` and `common/exception`.

## Requirements
- JDK 25
- No separately installed Maven is required; use the Maven Wrapper.
- SQLite is stored in the current Windows user's persistent application-data directory (`%APPDATA%/BookstoreManagementSystem/bookstore.db` by default). Set `BMS_DATABASE_PATH` to override it.
- The API binds to `127.0.0.1:8080` by default. It is intended for the local desktop UI and must not be exposed to the shop LAN.
- Session cookies are HttpOnly and SameSite=Lax. The local loopback HTTP deployment uses a non-Secure cookie; do not expose this HTTP API beyond loopback.
- The default idle session timeout is 30 minutes. Set `BMS_SESSION_TIMEOUT` to a Spring duration such as `15m` or `1h` to override it.
- No internet connection is required at runtime.

## Run the backend (Windows PowerShell)
From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on loopback port 8080 by default.

## Run tests (Windows PowerShell)
```powershell
.\mvnw.cmd clean test
```

Tests use an isolated SQLite test configuration. They must not write to the shop database.

## Initial Admin account provisioning

A fresh installation is initialized by the bookstore owner through the local first-run setup screen; no SQLite editing, shared default password, or developer shell is required.

1. Start the backend from this directory:

   ```powershell
   .\\mvnw.cmd spring-boot:run
   ```

2. Start the frontend in a second terminal from the repository root:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

3. Open `http://localhost:3000`. When the database has no user accounts and has not been initialized, the dashboard displays the **Create your Admin account** screen. Enter the owner's name, a username, and a unique password, then confirm the password.
4. After the account is created, sign in through the normal login screen.

The setup API is protected by the same session-bound CSRF mechanism as other state-changing requests. The API binds to loopback (`127.0.0.1`) and is not intended to be exposed to the shop LAN. Passwords must contain 8–72 UTF-8 bytes and are BCrypt-hashed before persistence; plaintext passwords are not stored or logged.

The one-time marker and Admin row are committed in a single SQLite transaction. A singleton database constraint serializes concurrent initialization attempts: one request can succeed and subsequent requests receive HTTP 409. Any failure before commit rolls back both records, so the owner can safely retry after the backend is available again. If setup is unavailable because an account already exists, use the normal login or documented account recovery process; do not delete database rows or the marker manually.

For interrupted setup, first restart the backend and revisit the local dashboard. A failed provisioning transaction leaves setup available. If the SQLite file or schema is damaged, stop the backend, preserve a copy of the current database, and follow [the database backup and restore guide](docs/DATABASE-BACKUP-RESTORE.md) using a verified backup. Do not overwrite a newer database with an older backup without following the migration/recovery procedure.

The environment-variable bootstrap remains available only as an explicit operator/development fallback. It is disabled by default; never enable it with a committed or shared password:

```powershell
$env:BMS_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BMS_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BMS_BOOTSTRAP_ADMIN_PASSWORD = "use-a-unique-strong-local-password"
$env:BMS_BOOTSTRAP_ADMIN_FULL_NAME = "Store Administrator"
.\\mvnw.cmd spring-boot:run
```

Remove the variables after use. The operator bootstrap uses the same atomic provisioning service and cannot replace an existing account.

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
