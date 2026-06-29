$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "Starting TonePilot Lightroom Bridge..."
Write-Host "Default port: 47917"
node server.js
