$ErrorActionPreference = "SilentlyContinue"

$bridgeUrl = "http://127.0.0.1:33335/status"
$consoleUrl = "http://127.0.0.1:33335/agent-console"
$logRoot = Join-Path $env:USERPROFILE ".tonepilot-lightroom-bridge"
$logPath = Join-Path $logRoot "plugin-launcher.log"

New-Item -ItemType Directory -Path $logRoot -Force | Out-Null

function Write-LauncherLog {
  param([string] $Message)
  $line = "$(Get-Date -Format s) $Message"
  Add-Content -Path $logPath -Value $line -Encoding UTF8
}

function Test-Bridge {
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $bridgeUrl -TimeoutSec 2
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
  } catch {
    return $false
  }
}

Write-LauncherLog "Starting TonePilot Agent console"

if (-not (Test-Bridge)) {
  Write-LauncherLog "Local Runtime is not responding; trying to restart WSL user service tonepilot-local-runtime"
  $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
  if ($wsl) {
    Start-Process -FilePath "wsl.exe" -ArgumentList @("-e", "bash", "-lc", "systemctl --user restart tonepilot-local-runtime || systemctl --user restart tonepilot-lightroom-bridge") -WindowStyle Hidden
    Start-Sleep -Seconds 3
  } else {
    Write-LauncherLog "wsl.exe was not found; cannot auto-start TonePilot Local Runtime"
  }
}

if (Test-Bridge) {
  Write-LauncherLog "Local Runtime is ready; opening console $consoleUrl"
} else {
  Write-LauncherLog "Local Runtime is still not responding; opening console for diagnostics"
}

Start-Process $consoleUrl
