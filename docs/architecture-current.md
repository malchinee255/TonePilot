# TonePilot 当前架构说明

## 产品边界

TonePilot 按“用户端本地 Agent + 云端管理端”拆分：

- `clients/lightroom-classic/plugin`：Lightroom Classic Lua 插件，负责当前照片状态、预览、心跳和真实应用 Develop Settings。
- `clients/lightroom-classic/local-runtime`：用户端本地运行时，是 ReAct Agent 的核心。它读取 Lightroom 状态，维护会话上下文，检索可选知识库，调用 OpenAI/Qwen2，并根据模型 decision 调用 Lightroom 工具。
- `tonepilot-admin/backend`：管理端后端，负责素材导入、知识审核、Hybrid RAG、运行时设备与事件观测、评估和持久化。
- `tonepilot-admin/frontend`：管理端前端，面向管理员维护知识和查看观测数据。

Local Runtime 不再提供本地规则修图模式。没有配置模型时，本轮请求会返回明确错误并提示用户配置 API Key。知识库是可选增强：不可用时 Agent 仍可基于当前照片和用户上下文推理，但会在 ReAct 观察里记录“跳过/失败”的原因。

## ReAct 运行时

运行时每轮对话生成可见的 ReAct 行动轨迹：

```text
Thought 摘要：判断当前照片、用户意图和上下文
Action：inspect_lightroom_photo / retrieve_style_knowledge / call_chat_model / apply_lightroom_settings
Observation：照片状态、知识命中、模型输出摘要、Lightroom 工具结果
Decision：respond / ask_user / apply_global_adjustments / plan_local_adjustments
```

UI 展示主 Agent 的判断摘要和行动轨迹，不展示固定工作流清单。

## Hybrid RAG

管理端 RAG 使用混合检索抽象：

- 关键词分数：命中风格、场景、参数名和教程关键词。
- 向量分数：开发态为内存词频向量，后续可切换 embedding provider。
- Rerank 分数：保留 provider/model 配置，后续可接 Qwen/OpenAI/本地重排模型。
- 向量库：默认 `memory`，配置项预留 `milvus-uri` 和 collection。

默认配置位于 `tonepilot-admin/backend/src/main/resources/application.yml` 的 `tonepilot.rag.hybrid`。


## 本地基础设施

本地开发通过 `docker-compose.yml` 启动 MySQL、Redis、MinIO 和 Milvus。Milvus 作为调色知识库的默认向量数据库，管理端通过 `TONEPILOT_VECTOR_STORE=milvus` 和 `MILVUS_URI` 连接。
