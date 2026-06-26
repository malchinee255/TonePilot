# TonePilot

TonePilot 是一个非生成式摄影调色 Agent。它会分析照片、检索可复用的调色知识、生成可解释的 Lightroom 参数，支持多轮对话微调、预览渲染、评测调色结果，并导出 XMP 预设文件。

当前版本采用可替换的工程化 Agent 架构，既能本地零依赖跑通，也能逐步接入 Redis、MySQL、MinIO 和真实大模型：

- 后端：Spring Boot 3、Java 17，提供 API、工作流、持久化、可观测、评测和治理能力。
- 前端：Vue 3、TypeScript、Element Plus。
- Agent：采用可控状态机多 Agent 工作流，支持规则模式、OpenAI 和 Qwen2，并通过 LangChain4j 调用真实模型。
- RAG：第一版使用简单文本相似度检索，后续可替换为 Qdrant、Milvus、pgvector 或 Elasticsearch。
- 存储：支持本地文件系统和 MinIO 对象存储。
- 持久化：默认使用本地 H2 文件数据库，生产环境可切换 MySQL。
- 预览：使用 TonePilot 原生 Java2D 预览渲染，便于页面实时对比；真实 Lightroom Classic 联动预留连接器接口。

## 项目结构

```text
backend/      Spring Boot API，包含用户端链路、管理端链路、RAG、XMP、评测能力
frontend/     Vue 3 操作台，包含用户端调色链路和管理端知识库维护界面
docs/         架构说明和后续扩展计划
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认 API 地址：`http://localhost:8080/api`

## 大模型配置

默认模式是 `rule`，即本地规则版，不需要任何密钥。要启用真实大模型，可以通过环境变量切换。

OpenAI 和 Qwen2 的真实模型调用现在通过 LangChain4j 的 `OpenAiChatModel` 统一完成。文本任务和图片多模态分析共用同一套供应商配置。

```bash
# 使用 OpenAI
export TONEPILOT_AI_PROVIDER=openai
export OPENAI_API_KEY=你的_OpenAI_Key
export OPENAI_CHAT_MODEL=你的_OpenAI_文本模型
export OPENAI_VISION_MODEL=你的_OpenAI_视觉模型

# 使用阿里 Qwen2 / 通义千问 OpenAI 兼容模式
export TONEPILOT_AI_PROVIDER=qwen2
export DASHSCOPE_API_KEY=你的_阿里云_DashScope_Key
export QWEN2_CHAT_MODEL=你的_Qwen2_文本模型
export QWEN2_VISION_MODEL=你的_Qwen2_视觉模型
```

也可以在请求中临时指定供应商：

```bash
POST /api/photos/{photoId}/analyze?provider=openai
POST /api/photos/{photoId}/analyze?provider=qwen2
```

参数生成接口的请求体可以传入：

```json
{
  "photoId": 1,
  "targetStyle": "夜景电影感",
  "provider": "qwen2"
}
```

供应商取值：

- `rule`：本地规则版，不调用大模型。
- `openai`：OpenAI 官方接口。
- `qwen2`：阿里通义千问 OpenAI 兼容模式。

当大模型调用失败且 `TONEPILOT_AI_FALLBACK_ENABLED=true` 时，系统会自动回退到本地规则版，保证链路不断。

## 数据库持久化

默认使用 H2 文件数据库，数据保存在后端目录下的 `data/`，用于本地开发和演示。照片、照片分析、调色方案、工作流快照、LLM 调用日志和审计事件会写入数据库快照。

切换 MySQL：

```bash
export TONEPILOT_DB_URL=jdbc:mysql://localhost:3306/tonepilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
export TONEPILOT_DB_USERNAME=tonepilot
export TONEPILOT_DB_PASSWORD=tonepilot
export TONEPILOT_DB_DRIVER=com.mysql.cj.jdbc.Driver
```

本地启动 MySQL：

```bash
docker compose up -d mysql
```

## 对象存储配置

默认使用本地文件系统：

```bash
export TONEPILOT_STORAGE_TYPE=local
```

本地文件会保存在后端目录下的 `storage/`，访问路径仍是 `/files/...`。

如果要用 MinIO 模拟 OSS，可以先启动 MinIO：

```bash
cd /home/lvchanghong/Code/TonePilot
docker compose up -d minio
```

MinIO 控制台：

- 地址：`http://localhost:9001`
- 用户名：`tonepilot`
- 密码：`tonepilot123`

后端切到 MinIO：

```bash
export TONEPILOT_STORAGE_TYPE=minio
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=tonepilot
export MINIO_SECRET_KEY=tonepilot123
export MINIO_BUCKET=tonepilot
```

业务接口返回的文件地址仍然是 `/files/photos/...`、`/files/xmp/...`。当存储类型为 MinIO 时，后端会代理 `/files/**`，从 MinIO 读取对象并返回给前端。

## Redis 工作流上下文

调色工作流的上下文和 Agent trace 会写入 Redis，同时会写入数据库快照。Redis 不可用时会自动降级到数据库或本地缓存，保证开发环境不被外部依赖阻塞。

启动 Redis：

```bash
cd /home/lvchanghong/Code/TonePilot
docker compose up -d redis
```

相关配置：

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export TONEPILOT_WORKFLOW_REDIS_ENABLED=true
export TONEPILOT_WORKFLOW_TTL=24h
```

工作流 key 默认前缀是 `tonepilot:workflow:`。生成调色参数后，可以用返回结果里的 `rawResponse.workflowRunId` 查询上下文快照：

```bash
GET /api/agent/workflows/{workflowRunId}
```

## 多 Agent 工作流

用户端调色参数生成现在由 `TonePilotWorkflowOrchestrator` 编排，不再由单个 Service 临时串联。每次请求都会创建一个 `TonePilotAgentContext`，记录运行 id、当前 Agent、照片分析结果、RAG 命中、调色草稿、参数校验结果和每个节点的 trace。

当前用户端工作流：

```text
ImageAnalysisAgent
  -> RagRetrievalAgent
  -> ColorPlanningAgent
  -> ParamValidationAgent
```

上下文控制原则：

- 全局上下文保存完整运行状态，便于调试、回放和审计。
- 每个 Agent 只读取自己需要的上下文视图，避免把原图、知识库和中间结果无脑塞进模型上下文。
- RAG 只返回 topK 知识片段，调色规划 Agent 只接收照片分析摘要、目标风格和命中的知识。
- 参数校验 Agent 会检查并收敛 Lightroom 参数范围，防止模型输出过激参数。

生成调色参数后，可以通过返回结果里的 `rawResponse.workflowRunId` 查询本次工作流。查询结果优先来自 Redis，Redis 不可用时回退数据库和本地缓存：

```bash
GET /api/agent/workflows/{workflowRunId}
```

## 多轮调色会话

生成第一版调色参数后，前端会自动开启微调会话。用户可以继续输入“再亮一点”“暖一点”“降低绿色”“肤色更通透”等指令，后端会把自然语言转换为参数增量，重新渲染预览图，并返回最新参数变化列表。

会话状态会写入 `domain_snapshot`，服务重启后仍可按会话 id 查询；预览图会写入当前对象存储，本地模式和 MinIO 模式都使用 `/files/previews/...` 访问。

接口：

```bash
POST /api/tuning/sessions
GET /api/tuning/sessions/{sessionId}
POST /api/tuning/sessions/{sessionId}/messages
POST /api/tuning/sessions/{sessionId}/save
GET /api/tuning/lightroom/status
```

创建会话：

```json
{
  "photoId": 1,
  "adjustmentId": 1
}
```

发送微调消息：

```json
{
  "message": "再亮一点，整体暖一点",
  "provider": "rule"
}
```

返回的会话对象包含：

- `currentAdjustment`：当前参数草稿，保存前不会生成新的 `adjustmentId`。
- `latestDeltas`：本轮改动的参数、原值、新值、变化量和原因。
- `preview`：原图地址和调色预览图地址。
- `messages`：用户和 Agent 的多轮对话记录。

保存会话后，系统会创建新的 `ColorAdjustment`，之后可以继续评测和导出 XMP。

## Lightroom 联动边界

当前版本没有直接控制本地 Lightroom Classic。原因是 Lightroom 云 API 主要面向 Creative Cloud catalog 和云端资源，本地 Lightroom Classic 通常需要通过 Classic SDK / Lua 插件、XMP 文件或热文件夹桥接完成联动。

TonePilot 已提供 `LightroomConnector` 抽象和状态接口：

```bash
GET /api/tuning/lightroom/status
```

默认实现会返回 `tonepilot-native-preview`，表示当前使用项目内置预览渲染。后续可以在不改前端交互的情况下替换为：

- Lightroom Classic Lua 插件桥接：插件监听本地请求，应用 Develop 参数并导出渲染结果。
- XMP 热文件夹桥接：TonePilot 写入 XMP，Lightroom 监听导入并渲染。
- Lightroom 云 API：用于 Creative Cloud catalog 场景，不等同于直接调用本机 Lightroom。

## 可观测性与审计

系统会记录：

- LLM 调用日志：provider、model、任务类型、耗时、成功/失败、Prompt 摘要、响应摘要、错误信息。
- 审计事件：调色生成、benchmark 运行、鉴权失败、限流事件。

查询接口：

```bash
GET /api/observability/llm-calls?limit=50
GET /api/observability/audit-events?limit=50
```

## 自动评测

内置固定评测样本集，可以对不同 provider 做自动回归评测：

```bash
POST /api/evaluation/benchmark
```

请求示例：

```json
{
  "providers": ["rule", "openai", "qwen2"]
}
```

返回内容包含每个 provider 的 case 数、通过数、通过率、平均分和逐 case 问题列表。

## 生产治理

API Key 鉴权默认关闭，可以通过环境变量开启：

```bash
export TONEPILOT_API_KEY_ENABLED=true
export TONEPILOT_API_KEY=你的服务端密钥
```

开启后请求需要携带：

```text
X-TonePilot-Api-Key: 你的服务端密钥
```

接口限流默认开启：

```bash
export TONEPILOT_RATE_LIMIT_ENABLED=true
export TONEPILOT_RATE_LIMIT_RPM=120
```

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`

## MVP 接口范围

用户端链路：

- `POST /api/photos/upload`
- `POST /api/photos/{photoId}/analyze`
- `POST /api/rag/search`
- `POST /api/agent/generate-adjustment`
- `GET /api/agent/workflows/{workflowRunId}`
- `POST /api/evaluation/check`
- `POST /api/evaluation/benchmark`
- `POST /api/xmp/export`
- `POST /api/tuning/sessions`
- `GET /api/tuning/sessions/{sessionId}`
- `POST /api/tuning/sessions/{sessionId}/messages`
- `POST /api/tuning/sessions/{sessionId}/save`
- `GET /api/tuning/lightroom/status`
- `GET /api/observability/llm-calls`
- `GET /api/observability/audit-events`

调色知识库：

- `POST /api/knowledge`
- `GET /api/knowledge`
- `DELETE /api/knowledge/{id}`

管理端链路：

- `POST /api/admin/styles`
- `GET /api/admin/styles`
- `GET /api/admin/styles/{id}`
- `PUT /api/admin/styles/{id}`
- `DELETE /api/admin/styles/{id}`
- `POST /api/admin/style-samples/upload`
- `POST /api/admin/style-samples/{sampleId}/analyze`
- `POST /api/admin/style-samples/{sampleId}/generate-knowledge`
- `PUT /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge/{id}/approve`
- `POST /api/admin/knowledge/{id}/reject`
- `POST /api/admin/knowledge/{id}/disable`

## 设计说明

代码会刻意把领域决策和基础设施隔开：

- `agent`：照片分析、风格分析、调色参数规划等 Agent 适配器。
- `workflow`：可控状态机多 Agent 编排、统一 AgentNode、上下文和节点 trace。
- `tuning`：多轮调色会话、自然语言微调、参数差异记录。
- `render`：把 Lightroom 风格参数近似渲染为页面预览图。
- `lightroom`：Lightroom 外部连接器抽象。
- `observability`：LLM 调用日志、审计事件和 trace 关联。
- `persistence`：数据库快照仓储和启动水位恢复。
- `security`：API Key 鉴权和基础限流。
- `rag`：检索逻辑，第一版是文本相似度实现。
- `xmp`：把 Lightroom 参数 JSON 映射为 XMP 字段。
- `harness`：检查参数范围、输出结构和场景规则。
- `store`：内存仓储，作为 MySQL 和向量数据库持久化的本地替身。

这样本地不依赖外部服务也能运行，同时具备 Redis/MySQL/MinIO/真实模型的接入点，后续替换向量库、接入 Langfuse 或 OpenTelemetry 时不用大改业务代码。
