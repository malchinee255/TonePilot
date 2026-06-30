# Runtime Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 让本地运行时产生的用户输入、Agent 思考、大模型回复、知识库检索、Lightroom 工具调用和错误都能上报到管理端，并能按用户、会话和 Trace 追溯。

**Architecture:** 本地运行时只采集和上报事件，不落业务数据库；管理端作为统一观测存储和查询面，继续使用 runtime_event 表保存结构化事件。运行时启动或首次上报时注册设备身份，之后所有事件带 userId、deviceId、sessionId、traceId、eventType 和 payload。

**Tech Stack:** Spring Boot 3.3、Java 17、JdbcTemplate/MySQL、Vue 3、Element Plus、Log4j2。

---

### Task 1: 管理端运行时事件查询接口

**Files:**
- Modify: tonepilot-admin/backend/domain/src/main/java/com/tonepilot/domain/runtime/RuntimeEventQuery.java
- Modify: tonepilot-admin/backend/repository/src/main/java/com/tonepilot/repository/runtime/RuntimeIngestRepository.java
- Modify: tonepilot-admin/backend/application/src/main/java/com/tonepilot/application/runtime/RuntimeIngestService.java
- Modify: tonepilot-admin/backend/application/src/main/java/com/tonepilot/application/controller/RuntimeIngestController.java
- Modify: tonepilot-admin/backend/infrastructure/src/main/java/com/tonepilot/infrastructure/runtime/repository/JdbcRuntimeIngestRepository.java
- Test: tonepilot-admin/backend/starter/src/test/java/com/tonepilot/runtime/RuntimeIngestServiceTest.java

- [ ] 写失败测试：按 userId、sessionId、traceId、eventType、limit 查询事件。
- [ ] 实现 RuntimeEventQuery 和 repository/service/controller 查询方法。
- [ ] 保持旧 /api/runtime/events?userId= 兼容。

### Task 2: 运行时设备注册与事件上报客户端

**Files:**
- Modify: clients/lightroom-classic/local-runtime/infrastructure/src/main/java/com/tonepilot/infrastructure/admin/AdminRuntimeClient.java
- Modify: clients/lightroom-classic/local-runtime/infrastructure/src/main/java/com/tonepilot/infrastructure/observability/RuntimeTraceLogger.java
- Test: clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/admin/AdminRuntimeClientTest.java
- Test: clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/observability/RuntimeTraceLoggerTest.java

- [ ] 写失败测试：配置管理端地址时，事件上报前自动注册设备并复用返回的 userId/deviceId。
- [ ] 让 RuntimeTraceLogger 每条结构化日志同时调用 AdminRuntimeClient.recordEvent。
- [ ] 事件 payload 包含 traceId、sessionId、step、level、timestamp。

### Task 3: Agent、LLM、工具调用事件补齐

**Files:**
- Modify: clients/lightroom-classic/local-runtime/application/src/main/java/com/tonepilot/application/agent/RuntimeAgentOrchestrator.java
- Modify: clients/lightroom-classic/local-runtime/infrastructure/src/main/java/com/tonepilot/infrastructure/model/ModelRuntimeAgent.java
- Modify: clients/lightroom-classic/local-runtime/infrastructure/src/main/java/com/tonepilot/infrastructure/lightroom/repository/FileLightroomToolRepository.java
- Test: clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/agent/RuntimeAgentOrchestratorTest.java
- Test: clients/lightroom-classic/local-runtime/starter/src/test/java/com/tonepilot/runtime/agent/ModelRuntimeAgentTest.java

- [ ] 写失败测试：一次聊天会产生用户消息、Agent 返回、LLM 请求/响应、Lightroom 工具状态事件。
- [ ] 记录用户输入摘要和完整消息，记录模型原始回答，记录 Lightroom 参数变化和任务结果。
- [ ] 出错时记录 runtime.error 或对应步骤 error。

### Task 4: 管理端前端运行时 Trace 页面

**Files:**
- Modify: tonepilot-admin/frontend/src/api.ts
- Modify: tonepilot-admin/frontend/src/App.vue
- Modify: tonepilot-admin/frontend/src/styles.css

- [ ] 在“观测与评估”增加“运行时事件”表格。
- [ ] 支持按 userId、sessionId、traceId、eventType 过滤。
- [ ] 点事件查看 payload JSON，能看到模型回答和工具调用参数。

### Task 5: 验证与提交

- [ ] Run: cd tonepilot-admin/backend && mvn test
- [ ] Run: cd clients/lightroom-classic/local-runtime && mvn test
- [ ] Run: cd tonepilot-admin/frontend && npm run build
- [ ] 启动并探活 /api/runtime/events、/status、管理端前端。
- [ ] Commit and push.
