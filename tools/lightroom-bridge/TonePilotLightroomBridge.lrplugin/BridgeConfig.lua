local LrPathUtils = import "LrPathUtils"

local home = LrPathUtils.getStandardFilePath("home")

return {
    -- 必须和 tools/lightroom-bridge/server.js 的 TONEPILOT_LIGHTROOM_BRIDGE_ROOT 保持一致。
    bridgeRoot = LrPathUtils.child(home, ".tonepilot-lightroom-bridge"),
    bridgePort = 33335,
    pollSeconds = 1,
    writeAccessTimeoutSeconds = 30
}
