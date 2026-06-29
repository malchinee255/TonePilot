$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientRoot = Split-Path -Parent $scriptDir
$pluginSource = Join-Path $clientRoot "plugin\TonePilotLightroomBridge.lrplugin"
$moduleRoot = Join-Path $env:APPDATA "Adobe\Lightroom\Modules"
$pluginTarget = Join-Path $moduleRoot "TonePilotLightroomBridge.lrplugin"

if (!(Test-Path $pluginSource)) {
  throw "没有找到插件目录: $pluginSource"
}

New-Item -ItemType Directory -Path $moduleRoot -Force | Out-Null

if (Test-Path $pluginTarget) {
  Remove-Item -LiteralPath $pluginTarget -Recurse -Force
}

Copy-Item -LiteralPath $pluginSource -Destination $pluginTarget -Recurse

Write-Host "TonePilot Lightroom plugin installed to:"
Write-Host $pluginTarget
Write-Host "Please restart Lightroom Classic, or reload it from File > Plug-in Manager."
