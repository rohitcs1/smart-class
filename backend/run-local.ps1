$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Get-Content ".env" | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}

Write-Host "Starting ELNEB backend with local environment..."
& mvn spring-boot:run
