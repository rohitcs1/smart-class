$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Get-Content ".env" | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}

$port = [int]($env:PORT)
if ($port -eq 0) {
    $port = 8080
}
$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $owner = Get-Process -Id $listener[0].OwningProcess -ErrorAction SilentlyContinue
    Write-Host "ELNEB backend is already running on port $port (PID $($listener[0].OwningProcess), process $($owner.ProcessName))."
    Write-Host "Use http://localhost:$port/api/health or stop that process before restarting."
    exit 0
}

Write-Host "Starting ELNEB backend with local environment..."
& mvn spring-boot:run
