local LrFileUtils = import "LrFileUtils"
local LrPathUtils = import "LrPathUtils"

local function bridgeRoot()
    return LrPathUtils.child(LrPathUtils.getStandardFilePath("home"), ".tonepilot-lightroom-bridge")
end

local function writeDiagnostic(name, content)
    local root = bridgeRoot()
    if not LrFileUtils.exists(root) then
        LrFileUtils.createDirectory(root)
    end
    local file = io.open(LrPathUtils.child(root, name), "w")
    if file then
        file:write(tostring(content))
        file:close()
    end
end

local ok, errorMessage = pcall(function()
    writeDiagnostic("plugin-init.txt", "TonePilot Lightroom Bridge init at " .. tostring(os.time()))
    local existingWorker = _G.TonePilotBridgeWorker
    if existingWorker ~= nil and existingWorker.stop ~= nil then
        existingWorker.stop()
        writeDiagnostic("worker-replaced.txt", "TonePilot Bridge Worker replaced at " .. tostring(os.time()))
    end
    local bridgeWorkerPath = LrPathUtils.child(_PLUGIN.path, "BridgeWorker.lua")
    local BridgeWorker = dofile(bridgeWorkerPath)
    _G.TonePilotBridgeWorker = BridgeWorker
    BridgeWorker.start()
end)

if not ok then
    writeDiagnostic("plugin-error.txt", errorMessage)
end
