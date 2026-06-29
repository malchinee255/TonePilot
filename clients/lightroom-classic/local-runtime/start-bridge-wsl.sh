#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

windows_user="${TONEPILOT_WINDOWS_USER:-}"
if [[ -z "$windows_user" ]] && command -v cmd.exe >/dev/null 2>&1; then
  windows_user="$(cmd.exe /c echo %USERNAME% 2>/dev/null | tr -d '\r\n')"
fi
windows_user="${windows_user:-lvchanghong}"

windows_home="${TONEPILOT_WINDOWS_HOME:-/mnt/c/Users/${windows_user}}"
lightroom_root="${TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT:-C:\\Users\\${windows_user}\\.tonepilot-lightroom-bridge}"

export TONEPILOT_LIGHTROOM_BRIDGE_PORT="${TONEPILOT_LIGHTROOM_BRIDGE_PORT:-33335}"
export TONEPILOT_LIGHTROOM_BRIDGE_HOST="${TONEPILOT_LIGHTROOM_BRIDGE_HOST:-0.0.0.0}"
export TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL="${TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL:-http://127.0.0.1:${TONEPILOT_LIGHTROOM_BRIDGE_PORT}}"
export TONEPILOT_LIGHTROOM_BRIDGE_ROOT="${TONEPILOT_LIGHTROOM_BRIDGE_ROOT:-${windows_home}/.tonepilot-lightroom-bridge}"
export TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT="$lightroom_root"

echo "[TonePilot Local Runtime] WSL 启动"
echo "[TonePilot Local Runtime] 运行时目录: ${TONEPILOT_LIGHTROOM_BRIDGE_ROOT}"
echo "[TonePilot Local Runtime] Lightroom 任务目录: ${TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT}"
echo "[TonePilot Local Runtime] 监听地址: ${TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL}"

node server.js
