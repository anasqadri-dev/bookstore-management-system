# Local API and session security verification

The desktop application is designed to expose its Spring Boot API only to the
same Windows machine. The default production configuration binds the API to
`127.0.0.1:8080`; the local frontend uses the loopback address.

## Verify the listening interface on Windows

Start the backend with the production configuration, then run PowerShell from
the backend directory:

```powershell
.\scripts\verify-local-security.ps1
```

The script fails if the API is listening on a wildcard or LAN interface. It
does not replace the separate-device acceptance test.

## Verify LAN isolation

1. Find the Windows machine's LAN IPv4 address with `ipconfig`.
2. From a second device on the same network, run
   `Test-NetConnection -ComputerName <server-lan-ip> -Port 8080`.
3. Confirm `TcpTestSucceeded` is `False`.
4. From the Windows host, confirm
   `Test-NetConnection -ComputerName 127.0.0.1 -Port 8080` succeeds.
5. Repeat with the packaged desktop runtime once Electron packaging is
   available.

Do not open port 8080 in Windows Firewall as a workaround. If a controlled
development setup needs a different bind address, make that override explicit
and never include it in the packaged production configuration.

## Session cookie policy

The default policy is:

- `HttpOnly=true`: frontend JavaScript cannot read the session identifier.
- `SameSite=Lax`: cross-site cookie sending is restricted.
- `Secure=false`: required for the current local HTTP loopback transport.
  Revisit this when the desktop transport changes to HTTPS.
- `BMS_SESSION_TIMEOUT=30m`: configurable idle timeout; the default is 30
  minutes.

The session cookie must be inspected in the real browser/runtime because
MockMvc does not reproduce every embedded-container cookie behavior. After
login, inspect the `Set-Cookie` response in browser developer tools and
confirm HttpOnly and SameSite=Lax. Confirm the Secure attribute matches the
transport mode. Wait beyond a deliberately shortened test timeout and verify
that a protected API returns 401; then verify logout also invalidates the
server-side session.

## Evidence boundaries

Automated configuration tests verify the checked-in defaults. The Windows
socket script verifies the actual bind address. The second-device connection
test, packaged Electron test, and real-browser idle-expiry/cookie inspection
must be performed on the target Windows installation before closing the
related release-acceptance issues.
