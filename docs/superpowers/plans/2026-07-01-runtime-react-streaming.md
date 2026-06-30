# Runtime ReAct Streaming Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Lightroom 本地运行时从一次性模型调用改成可观测的 ReAct 执行过程，并用流式事件展示给用户端。

**Architecture:** 运行时保留当前轻量 DDD 分层，在 domain 增加 ReAct 事件模型，在 application orchestrator 中按 Observation、Thought、Action、Observation、Final 的顺序发出事件。普通 `/chat` 返回完整结果和事件列表，新的 `/chat/stream` 使用 SSE 推送同一批事件，前端优先使用流式接口。

**Tech Stack:** Java 17、Spring MVC `SseEmitter`、JUnit 5、Mockito、原生 HTML/JavaScript fetch streaming。

---

### Task 1: ReAct 事件模型和普通接口事件列表

**Files:**
- Create: `clients/lightroom-classic/local-runtime/domain/src/main/java/com/tonepilot/domain/agent/AgentReactEvent.java`
- Modify: `clients/lightroom-classic/local-runtime/application/src/main/java/com/tonepilot/application/agent/RuntimeAgentOrchestrator.java`
- Test: `clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/agent/RuntimeAgentOrchestratorTest.java`

- [ ] 写失败测试：`chat` 的 `data.reactEvents` 必须包含 `agent.started`、`agent.observation`、`knowledge.retrieved`、`model.request`、`agent.thought`、`agent.final`。
- [ ] 新增 `AgentReactEvent` record，包含 `type`、`title`、`content`、`payload`、`createdAt`。
- [ ] 在 orchestrator 内部维护事件列表，并在关键节点 emit，同时写入 `RuntimeTraceLogger` 和管理端事件。
- [ ] 运行定向测试通过。

### Task 2: SSE 流式接口

**Files:**
- Modify: `clients/lightroom-classic/local-runtime/application/src/main/java/com/tonepilot/application/controller/LocalRuntimeController.java`
- Modify: `clients/lightroom-classic/local-runtime/application/src/main/java/com/tonepilot/application/agent/RuntimeAgentOrchestrator.java`

- [ ] 新增 `chat(payload, Consumer<AgentReactEvent>)` 重载，让 SSE 和普通接口复用同一套 ReAct 逻辑。
- [ ] 新增 `POST /api/lightroom-agent/chat/stream`，返回 `text/event-stream`，逐个发送 ReAct event，并最终发送 `agent.final`。
- [ ] 流式异常时发送 `agent.error`，并关闭 emitter。

### Task 3: 用户端流式 ReAct 展示

**Files:**
- Modify: `clients/lightroom-classic/local-runtime/starter/src/main/resources/static/agent-console.html`
- Test: `clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/api/AgentConsolePageTest.java`

- [ ] 写失败测试：页面包含 `/api/lightroom-agent/chat/stream`、`appendReactEvent`、`agent.thought`、`agent.final`。
- [ ] 前端发送后立即清空输入框，创建“思考中”卡片。
- [ ] 使用 fetch readable stream 读取 SSE 文本，按事件类型追加可观察思考、知识库、模型、工具调用、最终结果。
- [ ] 移除重复的 `sendMessage`/`renderAgentResult` 定义。

### Task 4: 验证与提交

**Files:**
- Runtime Maven reactor

- [ ] 运行：`mvn -f clients/lightroom-classic/local-runtime/pom.xml test`。
- [ ] 检查 git 状态，仅包含本轮 ReAct/streaming 改动。
- [ ] 提交并推送：`feat-runtime-react-streaming`。
