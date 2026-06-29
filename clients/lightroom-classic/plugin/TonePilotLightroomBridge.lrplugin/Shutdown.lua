local BridgeWorker = _G.TonePilotBridgeWorker

if BridgeWorker ~= nil and BridgeWorker.stop ~= nil then
    BridgeWorker.stop()
    _G.TonePilotBridgeWorker = nil
end
