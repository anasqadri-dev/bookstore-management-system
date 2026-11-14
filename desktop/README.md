# Windows desktop runtime (in progress)

This folder contains the Electron main process and packaging configuration for the local-first Windows application.

## Runtime design

1. Electron starts the bundled Java runtime and Spring Boot JAR.
2. It polls `GET http://127.0.0.1:8080/api/health` until both the backend and SQLite database report UP.
3. Electron starts the Next.js standalone server on `127.0.0.1:3000` using Electron's embedded Node runtime.
4. The BrowserWindow opens only after both services are ready.
5. The renderer has Node integration disabled, context isolation and sandbox enabled, and a minimal informational preload bridge.
6. Closing the application stops both managed child processes. SQLite is stored under Electron's per-user application-data directory, not the install folder.

The backend CORS allow-list is restricted to the exact local renderer origin. Renderer navigation and new windows are restricted to that origin.

## Local development

Prerequisites: Node.js 22, JDK 25, and the repository's frontend/backend dependencies.

From the repository root:

```powershell
cd backend-bookstore-management-system
.\mvnw.cmd clean package -DskipTests
cd ..\frontend
npm ci
npm run build:desktop
cd ..\desktop
npm install
npm test
npm start
```

The development runtime uses `JAVA_HOME` or `java` from PATH and the backend JAR under `backend-bookstore-management-system/target`.

## Windows packaging

The Windows CI workflow builds the backend JAR, creates a trimmed Java runtime with `jlink`, builds the Next.js standalone output, installs the pinned Electron tooling, runs runtime tests, and creates an NSIS installer.

Use the [Windows release acceptance checklist](WINDOWS-ACCEPTANCE-CHECKLIST.md) to record clean-machine installation, offline operation, persistence, backup/restore, printer, scaling, shutdown, upgrade, and uninstall results. The generated installer is not yet a release-approved artifact. A clean Windows 10/11 installation, offline launch, restart/persistence, backup/restore, printer selection and paper-size, shutdown/orphan-process, upgrade, and uninstall test is still required before issues #232, #279, and #291 can be closed.

Uninstall intentionally preserves the per-user database. The installer does not store SQLite data in the installation directory.
