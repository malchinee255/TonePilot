# TonePilot Lightroom Classic 客户端

这个目录是 TonePilot 的 Lightroom Classic 用户端，采用接近 Neurapix 的产品结构：Lightroom 插件作为入口，本地 Bridge 作为连接器，后端作为 Agent 大脑。

```text
clients/lightroom-classic/
├── bridge/     本地 Bridge 服务、Agent 控制台、安装脚本和测试
└── plugin/     Lightroom Classic Lua 插件源码
```

## 边界

Bridge 负责本地电脑上的插件协作：

- 读取 Lightroom 插件写入的当前选中照片快照。
- 提供深灰色 Agent 控制台页面。
- 把用户调色指令转发给后端 `/api/lightroom-agent/tune`。
- 把后端返回的 Develop Settings 写入任务结果。
- 由 Lightroom 插件在 Lightroom 进程内调用 `photo:applyDevelopSettings` 应用参数。

Bridge 不承担后端渲染兼容接口，也不要求摄影师在浏览器上传照片。

## 安装插件

在 Windows PowerShell 中运行：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\clients\lightroom-classic\bridge
.\install-plugin.ps1
```

然后打开 Lightroom Classic 插件管理器，确认 `TonePilot Lightroom Bridge` 已启用。

## 启动 Bridge

推荐在 WSL 中启动：

```bash
cd /home/lvchanghong/Code/TonePilot/clients/lightroom-classic/bridge
chmod +x start-bridge-wsl.sh
./start-bridge-wsl.sh
```

默认配置：

```text
监听地址：http://127.0.0.1:33335
任务目录：/mnt/c/Users/lvchanghong/.tonepilot-lightroom-bridge
后端地址：http://127.0.0.1:8080
```

也可以在 Windows PowerShell 中启动：

```powershell
cd C:\Users\lvchanghong\Documents\摄影调色agent\TonePilot-scaffold\clients\lightroom-classic\bridge
.\start-bridge.ps1
```

## Lightroom 用户入口

```text
文件 > 增效工具附加功能 > 打开 TonePilot Agent 控制台
```

这个入口会检查 Bridge 并打开：

```text
http://127.0.0.1:33335/agent-console
```

控制台支持：

- 显示 Lightroom 当前选中照片信息。
- 显示当前 Develop 参数。
- 以对话形式输入调色指令。
- 选择 `rule`、`openai`、`qwen2` 模型供应商。
- 展示 Agent 回复和参数变化。
- 继续多轮微调当前照片。

## 本地协议

状态：

```bash
curl http://127.0.0.1:33335/status
```

当前照片：

```bash
curl http://127.0.0.1:33335/api/lightroom/selected-photo
```

Agent 调色：

```bash
curl -X POST http://127.0.0.1:33335/api/lightroom-agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"夜景电影感，再亮一点","provider":"rule"}'
```

Bridge 会调用后端：

```text
POST http://127.0.0.1:8080/api/lightroom-agent/tune
```

## 验证

```bash
npm test
npm run check
```
