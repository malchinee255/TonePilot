# TonePilot

TonePilot 是一个非生成式 AI 摄影调色 Agent。系统不直接生成图片，而是模拟摄影师的调色判断，输出可解释、可校验、可落地到 Lightroom Classic 的调色参数。

当前工程保留两个产品端：

- 管理端：Web 管理台，用于维护调色风格、样片、知识库、审核状态、可观测日志和自动评测。
- 插件端：Lightroom Classic 用户端，摄影师在 Lightroom 中选中照片，通过 Agent 对话完成修图，并由插件把参数应用到当前照片。

后端作为 Agent 编排与管理服务，不提供浏览器修图工作台，不负责图片渲染，也不直接控制 Lightroom UI。真实修图效果以 Lightroom Classic 当前照片的 Develop Settings 为准。

## 快速启动

推荐在 WSL 目录中运行项目：

```bash
cd /home/lvchanghong/Code/TonePilot
```

启动本地依赖和后端：

```bash
chmod +x scripts/start-local-compose.sh
./scripts/start-local-compose.sh
```

脚本会启动 Docker Compose 中的 MySQL、Redis、MinIO，并启动后端服务。默认 API 地址：

```text
http://localhost:8080
```

只快速验证 Java 服务时，也可以使用 H2 和本地文件存储：

```bash
cd backend
mvn spring-boot:run
```

启动管理端：

```bash
cd frontend
npm install
npm run dev
```

默认管理端地址：

```text
http://localhost:5173
```

只启动基础设施：

```bash
docker compose up -d redis mysql minio
```

依赖用途：

- Redis：保存多 Agent 工作流上下文和 trace，支持分布式内存与服务重启后的上下文恢复。
- MySQL：生产形态关系数据库。开发时可用 H2 文件数据库。
- MinIO：本地模拟 OSS，用于管理端样片等文件对象存储。

## Lightroom 插件端

插件运行在 Windows 的 Lightroom Classic 中，Bridge 服务推荐运行在 WSL：

```bash
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/bridge
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

第一次安装插件需要在 Windows PowerShell 执行：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\clients\lightroom-classic\bridge
.\install-plugin.ps1
```

Lightroom 中的入口：

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

插件端链路：

```text
Lightroom 当前选中照片
  -> Lua 插件读取照片信息和 Develop 参数
  -> Bridge 打开 Agent 控制台
  -> 用户用对话描述调色意图
  -> Bridge 调用后端 /api/lightroom-agent/tune
  -> 后端生成参数、参数差异和回复
  -> Bridge 写入任务文件
  -> Lua 插件调用 photo:applyDevelopSettings 应用到当前照片
```

Bridge 默认地址：

```text
http://127.0.0.1:33335
```

检查 Bridge 状态：

```bash
curl http://127.0.0.1:33335/status
```

查看当前 Lightroom 选中照片状态：

```bash
curl http://127.0.0.1:33335/api/lightroom/selected-photo
```

## 当前修图能力

已真实支持的是 Lightroom 全局 Develop Settings：

- 基础：曝光、对比度、高光、阴影、白色色阶、黑色色阶、色温、色调、纹理、清晰度、去朦胧、自然饱和度、饱和度。
- HSL：红、橙、黄、绿、浅绿、蓝、紫、洋红的色相、饱和度、明亮度。
- 效果：颗粒、暗角。
- 扩展项：曲线、锐化、降噪、颜色分级、镜头校正、透视变换、暗角细项、颗粒细项、相机校准、裁剪和方向等全局参数。

暂未真实自动应用：

- Lightroom AI 天空、主体、背景等局部蒙版。
- 修复画笔和内容识别移除。
- 局部区域参数写回。

后续可以先支持局部修图计划展示，再实验 Lightroom SDK 或插件可行的真实落地方式。

## 大模型配置

默认使用 `rule` 本地规则模式，不需要模型密钥。真实模型通过 LangChain4j 的 OpenAI 兼容接口接入，当前支持 OpenAI 和阿里 Qwen2。

可以复制后端示例配置：

```bash
cd /home/lvchanghong/Code/TonePilot/backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

然后在 `application-local.yml` 里填写 `tonepilot.ai.openai` 或 `tonepilot.ai.qwen2`。

OpenAI 环境变量：

```bash
export TONEPILOT_AI_PROVIDER=openai
export OPENAI_API_KEY=你的_OpenAI_Key
export OPENAI_CHAT_MODEL=你的_OpenAI_文本模型
export OPENAI_VISION_MODEL=你的_OpenAI_视觉模型
```

阿里 Qwen2 环境变量：

```bash
export TONEPILOT_AI_PROVIDER=qwen2
export DASHSCOPE_API_KEY=你的_DashScope_Key
export QWEN2_CHAT_MODEL=你的_Qwen2_文本模型
export QWEN2_VISION_MODEL=你的_Qwen2_视觉模型
```

插件端请求也可以传入 `provider`，临时选择 `rule`、`openai` 或 `qwen2`。

## 项目结构

```text
TonePilot/
├── backend/                         Spring Boot 后端
│   ├── agent/                       规则模式和模型版 Agent 适配
│   ├── ai/                          LangChain4j 与 OpenAI 兼容模型客户端
│   ├── colorgrading/domain/         调色参数和值对象
│   ├── domain/                      照片、风格、样片、知识等通用领域对象
│   ├── evaluation/                  自动评测
│   ├── lightroom/                   Lightroom 插件端 DDD 分层
│   │   ├── domain/                  参数差异和调色计划
│   │   ├── application/             调色应用服务和意图规划
│   │   ├── infrastructure/          Develop Settings 映射
│   │   └── interfaces/              插件端 REST API
│   ├── observability/               LLM 调用日志和审计事件
│   ├── persistence/                 数据库快照和恢复
│   ├── service/                     管理端、RAG、样片、风格等业务服务
│   ├── web/                         管理端、插件端、评测和观测 API
│   └── workflow/                    多 Agent 编排、上下文和 trace
├── frontend/                        Vue 3 管理端
├── clients/
│   └── lightroom-classic/
│       ├── bridge/                  本地 Bridge 服务、Agent 控制台、安装脚本和测试
│       │   ├── server.js            Bridge 启动入口
│       │   └── src/bridge-runtime.js Bridge 运行主体
│       └── plugin/                  Lightroom Classic Lua 插件源码
├── docs/                            架构说明
├── scripts/                         本地启动脚本
└── docker-compose.yml               Redis、MySQL、MinIO 本地依赖
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

观测与评测：

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
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/bridge
npm test
npm run check
```
