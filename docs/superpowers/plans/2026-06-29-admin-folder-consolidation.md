# Admin Folder Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将管理端前后端收拢到 `tonepilot-admin/`，让云端管理端和 Lightroom 用户端目录边界更清楚。

**Architecture:** `tonepilot-admin/backend` 承担管理端后端，`tonepilot-admin/frontend` 承担管理端 Web。`clients/lightroom-classic/local-runtime` 继续作为用户本机 Agent 运行时，`clients/lightroom-classic/plugin` 继续作为 Lightroom 插件源码。

**Tech Stack:** Spring Boot、Vue/Vite、Node.js、Lightroom Classic Lua 插件、Docker Compose。

---

### Task 1: Move Admin Directories

**Files:**
- Move: `backend/` -> `tonepilot-admin/backend/`
- Move: `frontend/` -> `tonepilot-admin/frontend/`

- [ ] 创建 `tonepilot-admin/`。
- [ ] 移动管理端后端和前端目录。
- [ ] 确认 `clients/lightroom-classic/local-runtime` 未移动。

### Task 2: Update Paths

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `scripts/start-local-compose.sh`

- [ ] 将 `cd backend` 改为 `cd tonepilot-admin/backend`。
- [ ] 将 `cd frontend` 改为 `cd tonepilot-admin/frontend`。
- [ ] 将项目结构说明改成 `tonepilot-admin/backend` 和 `tonepilot-admin/frontend`。
- [ ] 将管理端启动脚本里的后端目录改成新的路径。

### Task 3: Verify

**Commands:**
- `cd tonepilot-admin/backend && mvn test`
- `cd clients/lightroom-classic/local-runtime && npm test`
- `cd clients/lightroom-classic/local-runtime && npm run check`

- [ ] Windows 侧验证 Local Runtime 测试和语法检查。
- [ ] 同步到 WSL `/home/lvchanghong/Code/TonePilot`。
- [ ] WSL 侧验证后端 Maven 测试。
- [ ] WSL 侧验证 Local Runtime 测试和语法检查。

### Task 4: Commit

- [ ] `git add -A`
- [ ] `git commit -m "refactor consolidate admin app"`
- [ ] `git push origin master`
