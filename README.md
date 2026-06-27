# TonePilot

TonePilot 是一个非生成式摄影调色 Agent。系统不生成图片，而是模拟摄影师的调色判断，输出可解释、可校验、可落地到 Lightroom Classic 的 Develop 参数。

当前工程只保留两个产品端：

- 管理端：Web 管理台，用于维护风格库、样片、调色知识、审核状态、可观测日志和自动评测。
- 插件端：Lightroom Classic 用户端，摄影师在 Lightroom 中选中照片，通过 Agent 对话完成修图，并由插件直接应用 Develop Settings。

后端作为 Agent 编排与管理服务，不再提供浏览器修图工作台、后端图片渲染、旧 `/api/tuning` 会话接口或 XMP 导出入口。

## 启动项目

推荐在 WSL 目录中运行项目：

```bash
cd /home/lvchanghong/Code/TonePilot
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

默认 API 地址：`http://localhost:8080`

启动管理端：

```bash
cd frontend
npm install
npm run dev
```

默认管理端地址：`http://localhost:5173`

如需 Redis、MySQL、MinIO 等完整基础设施：

```bash
cd /home/lvchanghong/Code/TonePilot
docker compose up -d redis mysql minio
```

这些服务的用途：

- Redis：保存多 Agent 工作流上下文和 trace，支持分布式内存与服务重启后的上下文恢复。
- MySQL：替代默认 H2 文件数据库，作为生产形态关系数据库。
- MinIO：模拟 OSS，对管理端样片等文件做对象存储。

## Lightroom 插件端

插件安装在 Windows 的 Lightroom Classic 中，Bridge 服务推荐跑在 WSL：

```bash
cd /home/lvchanghong/Code/TonePilot/tools/lightroom-bridge
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

第一次安装插件需要在 Windows PowerShell 执行：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\tools\lightroom-bridge
.\install-plugin.ps1
```

Lightroom 中的用户入口：

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

插件端链路：

```text
Lightroom 当前选中照片
  -> 插件后台读取照片信息和 Develop 参数
  -> 本地 Bridge 打开 Agent 控制台
  -> 用户用对话描述调色意图
  -> Bridge 调用后端 /api/lightroom-agent/tune
  -> 后端生成参数和参数差异
  -> Bridge 写入任务结果
  -> 插件调用 photo:applyDevelopSettings 应用到当前照片
```

Lightroom 自身负责真实预览、前后对比、撤销和保存。浏览器管理端不承担摄影师修图入口。

Bridge 默认地址：

```text
http://127.0.0.1:33335
```

检查 Bridge 状态：

```bash
curl http://127.0.0.1:33335/status
```

查看当前 Lightroom 选中照片快照：

```bash
curl http://127.0.0.1:33335/api/lightroom/selected-photo
```

## 大模型配置

默认使用 `rule` 本地规则模式，不需要模型密钥。真实模型通过 LangChain4j 的 OpenAI 兼容接口接入，当前支持 OpenAI 和阿里 Qwen2。

OpenAI：

```bash
export TONEPILOT_AI_PROVIDER=openai
export OPENAI_API_KEY=你的_OpenAI_Key
export OPENAI_CHAT_MODEL=你的_OpenAI_文本模型
export OPENAI_VISION_MODEL=你的_OpenAI_视觉模型
```

阿里 Qwen2：

```bash
export TONEPILOT_AI_PROVIDER=qwen2
export DASHSCOPE_API_KEY=你的_DashScope_Key
export QWEN2_CHAT_MODEL=你的_Qwen2_文本模型
export QWEN2_VISION_MODEL=你的_Qwen2_视觉模型
```

插件端也可以在请求中传入 `provider`，临时选择 `rule`、`openai` 或 `qwen2`。

## 项目结构

```text
TonePilot/
├── backend/                  Spring Boot 后端
│   ├── agent/                规则模式和模型版 Agent 适配
│   ├── ai/                   LangChain4j 与 OpenAI 兼容模型客户端
│   ├── domain/               调色、照片、风格、样片等领域对象
│   ├── evaluation/           自动评测
│   ├── lightroomagent/       Lightroom 插件端调色接口、参数映射和微调规划
│   ├── observability/        LLM 调用日志、审计事件
│   ├── persistence/          数据库快照和恢复
│   ├── service/              管理端、RAG、样片、风格等业务服务
│   ├── web/                  管理端、插件端、评测和观测 API
│   └── workflow/             多 Agent 编排、上下文和 trace
├── frontend/                 Vue 3 管理端
│   └── src/                  风格库、知识库、样片管理、观测评估页面
├── tools/
│   └── lightroom-bridge/     本地 Bridge 服务和 Lightroom Classic 插件
├── docs/                     架构说明
└── docker-compose.yml        Redis、MySQL、MinIO 本地依赖
```

## 核心 API

插件端：

- `POST /api/lightroom-agent/tune`

管理端：

- `POST /api/admin/styles`
- `GET /api/admin/styles`
- `GET /api/admin/styles/{id}`
- `PUT /api/admin/styles/{id}`
- `DELETE /api/admin/styles/{id}`
- `POST /api/admin/style-samples/upload`
- `GET /api/admin/style-samples`
- `POST /api/admin/style-samples/{sampleId}/analyze`
- `POST /api/admin/style-samples/{sampleId}/generate-knowledge`
- `POST /api/knowledge`
- `GET /api/knowledge`
- `DELETE /api/knowledge/{id}`
- `GET /api/admin/knowledge`
- `PUT /api/admin/knowledge/{id}`
- `POST /api/admin/knowledge/{id}/approve`
- `POST /api/admin/knowledge/{id}/reject`
- `POST /api/admin/knowledge/{id}/disable`

观测与评估：

- `GET /api/observability/llm-calls`
- `GET /api/observability/audit-events`
- `POST /api/evaluation/benchmark`

## 后端 Agent 编排职责

后端不直接渲染照片，也不直接控制 Lightroom UI。它负责：

- 解析用户调色意图。
- 使用风格知识库进行 RAG 检索。
- 生成 Lightroom Develop 参数。
- 校验参数范围，避免越界或过激调整。
- 输出本轮参数 diff、原因和对话回复。
- 记录 LLM 调用、审计事件和自动评测结果。

## 验证命令

后端测试：

```bash
cd /home/lvchanghong/Code/TonePilot/backend
mvn test
```

前端构建：

```bash
cd /home/lvchanghong/Code/TonePilot/frontend
npm run build
```

Bridge 测试：

```bash
cd /home/lvchanghong/Code/TonePilot/tools/lightroom-bridge
npm test
npm run check
```
