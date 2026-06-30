#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

windows_profile="${TONEPILOT_WINDOWS_PROFILE:-}"
if [[ -z "$windows_profile" ]] && command -v cmd.exe >/dev/null 2>&1; then
  windows_profile="$(cmd.exe /c echo %USERPROFILE% 2>/dev/null | tr -d '\r\n')"
fi

windows_home="${TONEPILOT_WINDOWS_HOME:-}"
if [[ -z "$windows_home" && -n "$windows_profile" ]] && command -v wslpath >/dev/null 2>&1; then
  windows_home="$(wslpath -u "$windows_profile")"
fi
if [[ -z "$windows_home" ]]; then
  echo "[TonePilot Local Runtime] 无法自动识别 Windows 用户目录，请设置 TONEPILOT_WINDOWS_HOME。" >&2
  exit 1
fi

lightroom_root="${TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT:-}"
if [[ -z "$lightroom_root" && -n "$windows_profile" ]]; then
  lightroom_root="${windows_profile}\\.tonepilot-lightroom-bridge"
fi
if [[ -z "$lightroom_root" ]]; then
  echo "[TonePilot Local Runtime] 无法自动识别 Lightroom 任务目录，请设置 TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT。" >&2
  exit 1
fi

export TONEPILOT_LIGHTROOM_BRIDGE_PORT="${TONEPILOT_LIGHTROOM_BRIDGE_PORT:-33335}"
export TONEPILOT_LIGHTROOM_BRIDGE_HOST="${TONEPILOT_LIGHTROOM_BRIDGE_HOST:-0.0.0.0}"
export TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL="${TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL:-http://127.0.0.1:${TONEPILOT_LIGHTROOM_BRIDGE_PORT}}"
export TONEPILOT_LIGHTROOM_BRIDGE_ROOT="${TONEPILOT_LIGHTROOM_BRIDGE_ROOT:-${windows_home}/.tonepilot-lightroom-bridge}"
export TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT="$lightroom_root"

echo "[TonePilot Local Runtime] WSL 启动"
echo "[TonePilot Local Runtime] 运行时目录: ${TONEPILOT_LIGHTROOM_BRIDGE_ROOT}"
echo "[TonePilot Local Runtime] Lightroom 任务目录: ${TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT}"
echo "[TonePilot Local Runtime] 监听地址: ${TONEPILOT_LIGHTROOM_BRIDGE_PUBLIC_URL}"

echo "[TonePilot Local Runtime] 安装本地多模块依赖"
mvn -q -DskipTests install

echo "[TonePilot Local Runtime] 启动 starter 模块"
mvn -f starter/pom.xml spring-boot:run
