# Bookstore Management System (BMS)

A local-first bookstore management project for Qadri Book Store, Shahi Bazaar, Rohri. Developed for **Software Quality Engineering (CS304)** at Sukkur IBA University, Fall 2026.

> **Current status:** Active development. The Spring Boot backend and Next.js frontend exist, but the Windows Electron desktop installer/runtime has **not** been implemented. Several frontend management screens are still being integrated with the backend. Do not use unverified screens for live store transactions.

## What is in the repository?

| Component | Current implementation |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA/Hibernate, SQLite REST API |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 dashboard |
| Database | Local SQLite file stored in the Windows user application-data directory by default |
| Authentication | Backend session-based Admin/Staff login and authorization; first Admin is created through a one-time local setup screen |
| Desktop packaging | **Planned:** Electron/Windows packaging is not present |
| Frontend tests | **Planned:** no frontend test script/test runner currently configured |
| Backend tests | Maven Wrapper test suite |

Backend endpoints cover products, sales, school orders, suppliers, purchase orders, reports, authentication, administration, and database backup/restore. **An endpoint existing does not mean its frontend screen is fully connected.** Verify each workflow against the backend before relying on it.

## Repository structure

```text
bookstore-management-system/
├── backend-bookstore-management-system/  # Spring Boot API, tests, Maven Wrapper
├── frontend/                            # Next.js dashboard
├── docs/                                # SRS, SDS and SQE project documents
├── .github/                             # GitHub workflows and templates
├── CONTRIBUTING.md
└── README.md
```

The Electron runtime, cross-component test directory, and distributable Windows installer are **future work**, not existing repository folders.

## Run locally (Windows PowerShell)

**Prerequisites:** JDK **25**, Node.js **22** with npm, and Git. A separate Maven installation is **not required**: the backend includes `mvnw.cmd`. Initial dependency downloads may require internet access; the eventual bookstore runtime is intended to work locally/offline.

1. Clone the repository and enter it:

```powershell
git clone https://github.com/anasqadri-dev/bookstore-management-system.git
cd bookstore-management-system
```

2. Start the backend in the first terminal:

```powershell
cd backend-bookstore-management-system
.\mvnw.cmd spring-boot:run
```

The backend listens at `http://127.0.0.1:8080` by default. **For first-time setup**, open `http://127.0.0.1:3000` after starting the frontend. The local dashboard presents a one-time Admin setup form when the database has no user accounts. The owner chooses a unique password; no default password is shipped. See the [first-run setup guide](backend-bookstore-management-system/README.md#initial-admin-account-provisioning). Do not commit passwords or database files.

3. In a **second PowerShell terminal**, from the repository root, start the frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://127.0.0.1:3000`. The frontend defaults to the API at `http://127.0.0.1:8080`; see [frontend configuration](frontend/README.md) for `NEXT_PUBLIC_API_BASE_URL`, session cookies, CSRF, and troubleshooting.

### Run quality checks

Backend (from `backend-bookstore-management-system/`):

```powershell
.\mvnw.cmd clean test
```

Frontend (from `frontend/`):

```powershell
npm ci
npm run lint
npm run build
```

The [frontend CI workflow](.github/workflows/frontend-ci.yml) runs npm install, lint and build using Node 22. There is currently **no** `npm test` script; frontend automated test coverage is tracked separately. Backend tests use isolated test databases, not the bookstore's production SQLite file.

## Documentation

- [Software Requirements Specification (SRS)](docs/requirements/Bookstore-Management-System-SRS.pdf)
- [Software Design Specification (SDS)](docs/design/Bookstore-Management-System-SDS.pdf)
- [Project proposal](docs/Project%20Proposal.pdf)
- [SQE project requirements description](docs/SQE%20Project%20Requirements%20Description.pdf)
- [Backend architecture, setup, and Admin bootstrap](backend-bookstore-management-system/README.md)
- [Frontend setup and limitations](frontend/README.md)
- [Backend API contract](backend-bookstore-management-system/docs/API-CONTRACT.md)
- [Database backup and restore guide](backend-bookstore-management-system/docs/DATABASE-BACKUP-RESTORE.md)
- [Contribution and pull-request workflow](CONTRIBUTING.md)

## Quality and collaboration

The project follows requirements traceability, peer review, validation, automated backend testing, and frontend lint/build checks. New integrations should test success, validation failures, authorization failures, network errors, and persisted data behavior. Do not claim an issue is completed until its acceptance criteria are verified.

Work on a focused branch, open a pull request against `develop`, request peer review, and merge only after required checks and approval. Issues are closed manually after the merged change is verified.

## Team

Software Engineering, Sukkur IBA University — Fall 2026.

| Member | Primary responsibility |
| --- | --- |
| Muhammad Anas Qadri | Project coordination, architecture and integration |
| Isra Memon | UI/UX and feature development |
| Muhammad Waleed | Quality assurance and testing |

**Instructor:** Dr. Khursheed Ali

## License

License and distribution terms have not yet been finalized by the project team.
