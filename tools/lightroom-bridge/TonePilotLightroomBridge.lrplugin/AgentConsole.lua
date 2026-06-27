local LrDialogs = import "LrDialogs"
local LrHttp = import "LrHttp"
local LrPathUtils = import "LrPathUtils"
local LrTasks = import "LrTasks"

local Config = dofile(LrPathUtils.child(_PLUGIN.path, "BridgeConfig.lua"))

TonePilotDarkTheme = {
    -- 深灰 Web 控制台使用真实 CSS 渲染，避免 Lightroom Classic Lua 原生浮窗被系统主题强制显示成白色。
    name = "深灰 Agent 对话控制台",
    panel = "#2b2b2b",
    surface = "#343434",
    border = "#4a4a4a",
    text = "#d6d6d6",
    muted = "#9a9a9a",
    accent = "#b8c7d9"
}

local function bridgeConsoleUrl()
    local port = Config.bridgePort or 33335
    return "http://127.0.0.1:" .. tostring(port) .. "/agent-console"
end

LrTasks.startAsyncTask(function()
    local ok, errorMessage = pcall(function()
        local launcher = LrPathUtils.child(_PLUGIN.path, "StartBridgeAndConsole.ps1")
        LrTasks.execute('powershell.exe -NoProfile -ExecutionPolicy Bypass -File "' .. launcher .. '"')
        LrHttp.openUrlInBrowser(bridgeConsoleUrl())
    end)

    if not ok then
        LrDialogs.message(
            "TonePilot Agent 控制台",
            "无法打开深灰 Agent 控制台：" .. tostring(errorMessage) .. "\n请确认 Bridge 服务已启动。",
            "critical"
        )
    end
end)
