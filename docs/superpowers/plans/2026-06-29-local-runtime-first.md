# Local Runtime First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 TonePilot 用户端核心收敛到本地 Local Runtime，使 Lightroom 插件不再依赖管理端后端即可完成对话修图。

**Architecture:** 管理端后端只作为云端知识、样片、风格、评测和观测服务；Local Runtime 作为用户本机核心，负责模型配置、对话调色、Develop Settings 生成、Lightroom 任务写回和本地历史。Lightroom Lua 插件仍只负责读取当前照片和应用参数。

**Tech Stack:** Node.js Local Runtime、Lightroom Classic Lua 插件、Spring Boot 管理端后端、OpenAI-compatible Chat Completions API。

---

### Task 1: Runtime 目录边界

**Files:**
- Move: `clients/lightroom-classic/bridge` -> `clients/lightroom-classic/local-runtime`
- Modify: `clients/lightroom-classic/plugin/TonePilotLightroomBridge.lrplugin/BridgeConfig.lua`
- Modify: `clients/lightroom-classic/plugin/TonePilotLightroomBridge.lrplugin/StartBridgeAndConsole.ps1`
- Modify: `README.md`
- Modify: `docs/architecture.md`

- [ ] 将 `bridge` 目录移动为 `local-runtime`，保留 `server.js` 入口。
- [ ] 将 README 和架构文档中的 “Bridge 服务” 用户表达改为 “TonePilot Local Runtime”。
- [ ] 插件内部仍可使用 bridgeRoot 文件夹名，避免破坏 Lightroom 与任务文件兼容性。

### Task 2: Runtime 本地调色核心

**Files:**
- Create: `clients/lightroom-classic/local-runtime/src/local-rule-agent.js`
- Modify: `clients/lightroom-classic/local-runtime/src/bridge-runtime.js`
- Test: `clients/lightroom-classic/local-runtime/local-rule-agent.test.js`

- [ ] 实现 `createLocalAgentTune(payload)`，输入 `message`、`currentAdjustment` 和照片信息，输出 `assistantMessage`、`deltas`、`developSettings`。
- [ ] 支持基础调色意图：亮/暗、冷/暖、对比、饱和、肤色、绿色、蓝色、电影感、胶片感、锐化、降噪、曲线、颜色分级、镜头校正、透视和校准。
- [ ] 规则模式只修改用户明确指定的参数；“夜景电影感，再亮一点”不得输出 `Temperature` 或 `Tint`。
- [ ] `createAgentChat` 和文件任务 `processAgentRequest` 改为调用本地调色核心，不再请求管理端 `/api/lightroom-agent/tune`。

### Task 3: Runtime 模型配置和 BYOK

**Files:**
- Create: `clients/lightroom-classic/local-runtime/src/runtime-config.js`
- Create: `clients/lightroom-classic/local-runtime/src/model-agent.js`
- Modify: `clients/lightroom-classic/local-runtime/src/bridge-runtime.js`
- Test: `clients/lightroom-classic/local-runtime/runtime-config.test.js`

- [ ] 在 Runtime 根目录保存 `runtime-config.json`，字段包括 `provider`、`openai.apiKey`、`openai.baseUrl`、`openai.model`、`qwen2.apiKey`、`qwen2.baseUrl`、`qwen2.model`。
- [ ] 新增 `GET /api/runtime/config` 返回脱敏配置。
- [ ] 新增 `POST /api/runtime/config` 保存用户本地模型配置。
- [ ] `model-agent.js` 使用 OpenAI-compatible `/chat/completions` 调用 OpenAI 或 Qwen；模型失败时返回错误，由 Runtime 回退到本地规则并在回复中说明。
- [ ] Agent 控制台增加设置区域，用户可以选择模型厂商、填写 API Key、Base URL 和模型名。

### Task 4: 管理端后端职责收敛

**Files:**
- Delete: `backend/src/main/java/com/tonepilot/lightroom/**`
- Delete: `backend/src/test/java/com/tonepilot/lightroom/**`
- Modify: `README.md`
- Modify: `docs/architecture.md`

- [ ] 删除管理端后端中的 Lightroom 插件调色 REST API。
- [ ] 保留 `backend/src/main/java/com/tonepilot/colorgrading/domain`，因为管理端评测、知识和未来云同步仍需要调色参数对象。
- [ ] 文档中说明管理端后端不参与用户每次修图请求，只提供可选知识同步和管理能力。

### Task 5: 验证和提交

**Files:**
- Test all touched modules.

- [ ] 运行 `cd clients/lightroom-classic/local-runtime && npm test && npm run check`，预期 Node Runtime 测试通过。
- [ ] 运行 `cd backend && mvn test`，预期管理端后端测试通过。
- [ ] 同步到 WSL `<项目目录>`。
- [ ] 提交并推送到 `origin/master`。

