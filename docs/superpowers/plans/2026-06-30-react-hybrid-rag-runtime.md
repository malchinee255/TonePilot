# ReAct Hybrid RAG Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Lightroom Local Runtime 改造成 ReAct 风格 Agent，并让管理端 RAG 具备混合检索与向量模型扩展点。

**Architecture:** 本地运行时增加 ReAct 步骤模型、工具注册和知识检索 Action；模型输出包含可见思考摘要、下一步决策和工具调用建议。管理端增加 hybrid RAG 配置、内存向量索引和统一检索结果，为后续 Milvus/Embedding/Rerank 接入保留接口。

**Tech Stack:** Node.js local runtime、Spring Boot admin backend、Java records/services、node:test、Maven test。

---

### Task 1: 运行时模型调用改成 ReAct JSON

**Files:**
- Modify: `clients/lightroom-classic/local-runtime/src/model-agent.js`
- Test: `clients/lightroom-classic/local-runtime/test/model-agent.test.js`

- [ ] 写测试：模型响应包含 `agentThought`、`reactTrace`、`decision`、`developSettings` 时，运行时完整透传。
- [ ] 写测试：没有 API Key 时返回失败结果，不再 fallback 本地规则。
- [ ] 实现：更新 prompt schema 和 normalize 逻辑。
- [ ] 运行：`node --test clients/lightroom-classic/local-runtime/test/model-agent.test.js`

### Task 2: 运行时编排 ReAct 工具轨迹

**Files:**
- Modify: `clients/lightroom-classic/local-runtime/src/bridge-runtime.js`
- Test: `clients/lightroom-classic/local-runtime/test/plugin-console.test.js`

- [ ] 写测试：控制台代码包含 `renderReactTrace`、`agentThought`，不包含固定 `Agent 执行过程`。
- [ ] 实现：`createAgentChat` 构造 `inspect_lightroom_photo`、`retrieve_style_knowledge`、`apply_lightroom_settings` 轨迹。
- [ ] 实现：只有模型决策为 `apply_global_adjustments` 且存在参数时才调用 Lightroom。
- [ ] 运行：`node --test clients/lightroom-classic/local-runtime/test/plugin-console.test.js`

### Task 3: 管理端 Hybrid RAG 扩展点

**Files:**
- Modify: `tonepilot-admin/backend/src/main/resources/application.yml`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/rag/HybridRagProperties.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/rag/HybridRagService.java`
- Test: `tonepilot-admin/backend/src/test/java/com/tonepilot/rag/HybridRagServiceTest.java`

- [ ] 写测试：混合检索按关键词分数、向量分数和 rerank 权重排序。
- [ ] 实现：内存开发态混合打分。
- [ ] 配置：增加 embedding provider、Milvus、rerank 配置项。
- [ ] 运行：`mvn test`

### Task 4: 文档与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`

- [ ] 补充 ReAct 和 Hybrid RAG 说明。
- [ ] 运行 local runtime 与 admin backend 测试。
- [ ] 提交并推送。

