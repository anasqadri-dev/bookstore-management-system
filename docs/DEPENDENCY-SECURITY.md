# Dependency vulnerability review policy

The `Dependency Security Audit` GitHub Actions workflow checks the committed npm lockfile
and Maven dependency graph on dependency changes, weekly on Monday, and manually.

- **Blocking threshold:** npm high/critical advisories and Maven findings with CVSS >= 7.
- **Frontend:** `npm audit --package-lock-only --audit-level=high` includes direct and transitive locked packages.
- **Backend:** OWASP Dependency-Check Maven plugin generates a vulnerability report; CI fails for CVSS >= 7.
- **Triage:** Review the workflow logs and uploaded backend report. Confirm affected version,
  reachable usage, and a fixed version. Open a tracked issue and a focused dependency-update PR,
  then run the usual backend tests and frontend lint/build checks.
- **False positives:** Record the CVE, evidence, reviewer, and reason in the tracking issue.
  Do not suppress or lower the severity threshold without an approved, documented exception.
- **Scanner limitations:** NVD feed rate limits or outages can fail the Maven job. Treat scanner
  errors as incomplete checks, not as proof that dependencies are safe. Retry after feed recovery.
- **Security:** Actions have read-only repository permissions; no application credentials or
  database data are used. Dependency scanning requires network access only in GitHub CI.
  The bookstore application remains local/offline at runtime.

Before merging, verify the scan executes successfully and test a controlled vulnerable dependency
in an isolated branch to confirm the high-severity failure policy.
