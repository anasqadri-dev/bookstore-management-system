$ErrorActionPreference = "Stop"

$port = 8080
$listeners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue

if (-not $listeners) {
    throw "No process is listening on TCP port $port. Start the production-profile backend first."
}

$addresses = @($listeners | Select-Object -ExpandProperty LocalAddress -Unique)
$unexpected = @($addresses | Where-Object { $_ -notin @("127.0.0.1", "::1") })

if ($unexpected.Count -gt 0) {
    throw "The API is exposed on a non-loopback address: $($unexpected -join ', '). Expected only 127.0.0.1 and/or ::1."
}

Write-Host "PASS: TCP port $port listens only on loopback: $($addresses -join ', ')" -ForegroundColor Green
