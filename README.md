# TonePilot

TonePilot 是一个非生成式 AI 摄影调色 Agent。它不直接生成图片，而是模拟摄影师的调色判断，输出可解释、可校验、可落地到 Lightroom Classic 的调色参数。

## 产品架构

当前工程分成两个产品端：

- `clients/lightroom-classic`：Lightroom Classic 用户端，包括 Lua 插件和本地 Java Local Runtime。用户在 Lightroom 中选中照片，通过 Agent 对话完成真实调色。
- `tonepilot-admin`：云端管理端，包括 Spring Boot 后端和 Vue 管理端前端，用于维护风格知识库、样片、配置、用户设备、会话、Trace、工具调用和评估数据。

推荐架构：

```text
Lightroom Classic 插件
  -> TonePilot Local Runtime（本地 Java Agent）
      -> 本地规则 / 用户配置的大模型 / 可选管理端模型代理
      -> Lightroom 文件桥接任务
  -> TonePilot Admin（云端管理端）
      -> 用户、设备、知识库、配置、会话、Trace、工具调用记录
      -> MySQL / Redis / 对象存储
```

本地运行时是用户修图闭环里的 Agent 主体；管理端是云端控制面和数据中心。Local Runtime 不使用 SQLite，也不直接连接 MySQL/Redis；长期数据通过管理端 API 入库。

## 快速启动

推荐在 WSL 目录中运行项目：

```bash
cd /home/lvchanghong/Code/TonePilot
```

启动管理端依赖和后端：

```bash
chmod +x scripts/start-local-compose.sh
./scripts/start-local-compose.sh
```

脚本会启动 Docker Compose 中的 MySQL、Redis、MinIO，并启动管理端后端。默认 API 地址：

```text
http://localhost:8080
```

只启动管理端后端：

```bash
cd tonepilot-admin/backend
mvn spring-boot:run
```

启动管理端前端：

```bash
cd tonepilot-admin/frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

## Lightroom 用户端

安装插件需要在 Windows PowerShell 执行：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\clients\lightroom-classic\local-runtime
.\install-plugin.ps1
```

启动本地 Java Runtime：

```bash
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/local-runtime
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

默认地址：

```text
http://127.0.0.1:33335
```

Lightroom 入口：

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

## 本地运行时 Agent 流程

```text
Lightroom 选中照片
  -> Lua 插件写入 selected-photo.json、selected-preview.jpg 和当前 Develop Settings
  -> Local Runtime 读取当前照片状态
  -> 用户在 Agent 控制台输入修图意图
  -> Local Runtime 执行意图分析、照片类型判断、调色策略规划
  -> Local Runtime 使用本地规则、OpenAI、Qwen 或管理端模型代理生成本轮参数
  -> 参数校验，只输出需要修改的 Develop Settings
  -> Local Runtime 写入 apply-jobs
  -> Lua 插件调用 photo:applyDevelopSettings
  -> Lightroom 显示真实修图结果
  -> Local Runtime 可选上报管理端事件和 Trace
```

注意：没有被用户意图或 Agent 明确规划的参数不会被修改。例如用户只说“夜景电影感，再亮一点”，不会顺手改白平衡。

## 管理端职责

管理端负责：

- 用户和设备注册。
- 风格知识库、样片、Prompt、规则、模型配置管理。
- 运行时会话、消息、Trace、LLM 调用、Lightroom 工具调用记录。
- 自动评估、统计分析和可观测性。
- MySQL、Redis、MinIO 等服务端存储。

管理端不直接控制用户本地 Lightroom，也不是用户日常修图的强依赖。离线模式下，本地 Runtime 仍可用规则模式完成基础调色。

## 项目结构

```text
TonePilot/
├── tonepilot-admin/
│   ├── backend/                     Spring Boot 管理端后端
│   │   ├── agent/                   管理端 Agent 能力和模型适配
│   │   ├── ai/                      LangChain4j 与 OpenAI 兼容模型客户端
│   │   ├── colorgrading/domain/     调色参数和值对象
│   │   ├── observability/           LLM 调用日志和审计事件
│   │   ├── runtime/                 Local Runtime 设备、用户和事件接入
│   │   ├── service/                 知识库、样片、风格等业务服务
│   │   ├── web/                     管理端 API
│   │   └── workflow/                管理端多 Agent 编排和上下文
│   └── frontend/                    Vue 3 管理端前端
├── clients/
│   └── lightroom-classic/
│       ├── plugin/                  Lightroom Classic Lua 插件源码
│       └── local-runtime/           本地 Java Agent Runtime
│           ├── src/main/java/       运行时代码
│           ├── src/test/java/       运行时测试
│           ├── pom.xml
│           └── start-bridge-wsl.sh
├── docs/                            架构和计划文档
├── scripts/                         本地启动脚本
└── docker-compose.yml               Redis、MySQL、MinIO 本地依赖
```

## 核心 API

本地 Runtime：

- `GET /status`
- `GET /agent-console`
- `GET /api/lightroom/selected-photo`
- `GET /api/runtime/config`
- `POST /api/runtime/config`
- `POST /api/lightroom-agent/chat`

管理端 Runtime 接入：

- `POST /api/runtime/devices/register`
- `POST /api/runtime/events`
- `GET /api/runtime/events?userId=...`

管理端观测与评估：

- `GET /api/observability/llm-calls`
- `GET /api/observability/audit-events`
- `POST /api/evaluation/benchmark`

## 验证命令

管理端后端：

```bash
cd /home/lvchanghong/Code/TonePilot/tonepilot-admin/backend
mvn test
```

本地 Runtime：

```bash
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/local-runtime
mvn test
```

管理端前端：

```bash
cd /home/lvchanghong/Code/TonePilot/tonepilot-admin/frontend
npm run build
```
