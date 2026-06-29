local LrPathUtils = import "LrPathUtils"

local home = LrPathUtils.getStandardFilePath("home")

return {
    -- 必须和 clients/lightroom-classic/local-runtime/server.js 的 TONEPILOT_LIGHTROOM_BRIDGE_ROOT 保持一致。
    bridgeRoot = LrPathUtils.child(home, ".tonepilot-lightroom-bridge"),
    bridgePort = 33335,
    pollSeconds = 3,
    metadataDebugIntervalSeconds = 30,
    writeAccessTimeoutSeconds = 30
}
