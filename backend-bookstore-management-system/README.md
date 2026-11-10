# Backend — Bookstore Management System

## Architecture

The backend is a local Spring Boot REST API for the single-store, offline desktop application. It uses Java 25, Spring Boot 4.1.2, Spring Data JPA, Hibernate, and SQLite.

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
- SQLite is stored locally at `data/bookstore.db`.
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

Tests use the test-resource SQLite in-memory database configuration. They must not write to the shop's `data/bookstore.db` file.

## API conventions

Successful and failed controller responses use `ApiResponse<T>` with `success`, `message`, and `data` fields. Validation errors return HTTP 400, missing resources 404, data conflicts 409, unauthenticated requests 401, and forbidden requests 403.

All request DTOs must validate untrusted input at the controller boundary with Jakarta Bean Validation. Services must enforce business invariants again where those invariants protect data integrity.

## Quality rules

- Keep commits focused and small.
- Add automated tests for new business behavior and boundary conditions.
- Use transactions for multi-row business operations.
- Never persist or log plain-text passwords.
- Do not add fields or workflows that are absent from the approved SRS/SDS without documenting an agreed scope change.
