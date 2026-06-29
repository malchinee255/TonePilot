$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

if (-not $env:TONEPILOT_LIGHTROOM_BRIDGE_PORT) {
    $env:TONEPILOT_LIGHTROOM_BRIDGE_PORT = "33335"
}

Write-Host "Starting TonePilot Java Local Runtime..."
Write-Host "Default URL: http://127.0.0.1:$env:TONEPILOT_LIGHTROOM_BRIDGE_PORT"
mvn spring-boot:run
