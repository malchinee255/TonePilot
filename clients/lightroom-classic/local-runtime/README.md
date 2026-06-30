# TonePilot Lightroom Classic Local Runtime

## 当前行为

- Local Runtime 是用户端 ReAct Agent 的执行器：读取 Lightroom 当前照片，组织上下文，检索可选管理端知识库，调用 OpenAI/Qwen2，并按模型 decision 调用 Lightroom 工具。
- 本地规则修图模式已移除。没有配置模型 API Key 时，请求会返回明确错误，不会自动生成规则参数。
- 管理端知识库是可选增强。启用后，运行时会把用户意图、照片类型、相机/镜头和当前参数组成查询，到管理端 Hybrid RAG 检索调色知识。
- 每轮响应会包含主 Agent 判断、ReAct 行动轨迹、参数 diff、修图前后预览和版本记录。


这个目录是 TonePilot 的 Lightroom Classic 用户端本地运行时。用户层面只需要理解为“安装 TonePilot Lightroom 插件并启动 Agent”，运行时内部保留两部分：

- Lightroom Classic Lua 插件：在 Lightroom 进程内读取当前照片、写入心跳、应用 `photo:applyDevelopSettings`。
- TonePilot Local Runtime：在本机提供深灰 Agent 控制台、保存模型配置、调用 OpenAI/Qwen，并通过任务文件和插件协作。

管理端后端不是用户修图的必需依赖。它未来作为云端部署，用于维护风格知识库、样片、评测和观测。

```text
clients/lightroom-classic/
├── local-runtime/  本地运行时、Agent 控制台、安装脚本和测试
└── plugin/         Lightroom Classic Lua 插件源码
```

## 安装插件

在 Windows PowerShell 中运行：

```powershell
cd <项目目录>\clients\lightroom-classic\local-runtime
.\install-plugin.ps1
```

然后打开 Lightroom Classic 插件管理器，确认 `TonePilot Lightroom Bridge` 已启用。插件名称暂时保留 Bridge，是为了兼容已经安装过的 Lightroom 插件目录。

## 启动 Local Runtime

推荐在 WSL 中启动：

```bash
cd <项目目录>/clients/lightroom-classic/local-runtime
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

默认配置：

```text
监听地址：http://127.0.0.1:33335
任务目录：%USERPROFILE%\.tonepilot-lightroom-bridge（WSL 中会自动映射到对应用户目录）
模型配置：%USERPROFILE%\.tonepilot-lightroom-bridge\runtime-config.json
```

也可以在 Windows PowerShell 中启动：

```powershell
cd <项目目录>\clients\lightroom-classic\local-runtime
.\start-bridge.ps1
```

## Lightroom 用户入口

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

这个入口会检查 Local Runtime 并打开：

```text
http://127.0.0.1:33335/agent-console
```

控制台支持：

- 显示 Lightroom 当前选中照片信息和前后对比预览。
- 以对话形式输入调色指令并持续多轮微调。
- 使用 OpenAI 或阿里 Qwen2 等 OpenAI 兼容模型，需要先在本地模型设置中保存 API Key。
- 可在本地设置 OpenAI 或阿里 Qwen2 的 Base URL、模型名和 API Key。
- API Key 只写入本机 `runtime-config.json`，不发送给管理端。
- 只修改 Agent 本轮明确生成的 Lightroom Develop Settings，未指定参数保持不变。

## 本地协议

状态：

```bash
curl http://127.0.0.1:33335/status
```

当前照片：

```bash
curl http://127.0.0.1:33335/api/lightroom/selected-photo
```

读取模型配置：

```bash
curl http://127.0.0.1:33335/api/runtime/config
```

Agent 调色：

```bash
curl -X POST http://127.0.0.1:33335/api/lightroom-agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"夜景电影感，再亮一点，但不要改变白平衡","provider":"qwen2"}'
```

## 验证

```bash
npm test
npm run check
```
