# TonePilot Local Runtime

这个目录是 Lightroom Classic 用户端的本地 Java 运行时。它是用户修图闭环里的 Agent 主体，负责读取 Lightroom 当前状态、分析用户意图、生成调色参数、调用 Lightroom 插件应用参数，并把会话和工具调用事件按需上报管理端。

## 架构定位

```text
Lightroom Classic 插件
  -> TonePilot Local Runtime（本地 Java Agent）
      -> 本地规则 / 用户配置的大模型 / 可选管理端模型代理
      -> Lightroom 文件桥接任务
  -> TonePilot Admin（云端管理端）
      -> 用户、设备、知识库、配置、会话、Trace、工具调用记录
      -> MySQL / Redis / 对象存储
```

Local Runtime 不使用 SQLite，也不直接连接 MySQL/Redis。长期数据由管理端保存；本地只保存模型配置、Lightroom 桥接临时文件和应用任务结果。

## 启动

推荐在 WSL 中启动：

```bash
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/local-runtime
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

也可以在 Windows PowerShell 中启动：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\clients\lightroom-classic\local-runtime
.\start-bridge.ps1
```

默认地址：

```text
http://127.0.0.1:33335
```

## Lightroom 插件入口

在 Lightroom Classic 中打开：

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

控制台地址：

```text
http://127.0.0.1:33335/agent-console
```

## 本地 API

```text
GET  /status
GET  /agent-console
GET  /api/lightroom/selected-photo
GET  /api/runtime/config
POST /api/runtime/config
POST /api/lightroom-agent/chat
GET  /files/{fileName}
```

示例：

```bash
curl http://127.0.0.1:33335/status

curl -X POST http://127.0.0.1:33335/api/lightroom-agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"夜景电影感，再亮一点，但不要改白平衡"}'
```

## 核心流程

```text
1. Lightroom 插件写入 selected-photo.json、selected-preview.jpg 和当前 Develop Settings
2. 用户在 Agent 控制台输入修图意图
3. Local Runtime 读取当前照片和参数
4. Agent 分析意图、照片类型和调色方向
5. Agent 只生成本轮需要修改的 Develop Settings
6. Local Runtime 写入 apply-jobs/*.lua
7. Lightroom 插件调用 photo:applyDevelopSettings
8. Local Runtime 返回参数变化、应用结果和预览地址
9. Local Runtime 可选上报管理端事件
```

## 模型配置

默认使用 `rule` 本地规则模式，可以完全离线运行。OpenAI / Qwen2 配置保存在本机桥接目录：

```text
~/.tonepilot-lightroom-bridge/runtime-config.json
```

配置 API 会隐藏 API Key，只返回 `apiKeyConfigured`，避免控制台泄露密钥。

## 验证

```bash
mvn test
```
