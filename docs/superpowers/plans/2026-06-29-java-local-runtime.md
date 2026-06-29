# Java Local Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Lightroom 用户端的 TonePilot Local Runtime 从 Node 运行时迁移为 Java/Spring Boot Agent 项目，并让会话、调用、工具执行记录上报远程管理端持久化。

**Architecture:** Lightroom 插件继续使用本地文件桥接目录和 Local Runtime 通信；Local Runtime 负责 Agent 编排、模型/规则决策、Lightroom 工具调用；管理端负责用户、设备、会话、消息、Trace、工具调用入库。Local Runtime 不使用 SQLite，不保存长期会话存储，只保留 Lightroom 桥接所需的临时文件和本机模型配置。

**Tech Stack:** Java 17、Spring Boot 3.3、Jackson、JDK HttpClient、JUnit 5、Spring JDBC、MySQL/H2 schema。

---

### Task 1: 管理端运行时接入 API

**Files:**
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/runtime/RuntimeDeviceRegistrationRequest.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/runtime/RuntimeDeviceRegistrationResponse.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/runtime/RuntimeEventRequest.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/runtime/RuntimeIngestService.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/web/RuntimeIngestController.java`
- Modify: `tonepilot-admin/backend/src/main/resources/schema.sql`
- Modify: `tonepilot-admin/backend/src/main/resources/schema-mysql.sql`
- Test: `tonepilot-admin/backend/src/test/java/com/tonepilot/runtime/RuntimeIngestServiceTest.java`

- [ ] **Step 1: Write failing admin tests**

```java
@Test
void registersDeviceAndCreatesUserWhenFingerprintIsNew() {
    RuntimeDeviceRegistrationResponse response = service.registerDevice(
            new RuntimeDeviceRegistrationRequest("fp-1", "TonePilot Local Runtime", "127.0.0.1", Map.of("os", "Windows"))
    );
    assertThat(response.userId()).isNotBlank();
    assertThat(response.deviceId()).isNotBlank();
}

@Test
void appendsRuntimeEventForRegisteredDevice() {
    RuntimeDeviceRegistrationResponse response = service.registerDevice(
            new RuntimeDeviceRegistrationRequest("fp-2", "TonePilot Local Runtime", "127.0.0.1", Map.of())
    );
    service.recordEvent(new RuntimeEventRequest(
            response.userId(), response.deviceId(), "session.message", "session-1", Map.of("message", "夜景电影感")
    ));
    assertThat(service.listEvents(response.userId())).hasSize(1);
}
```

- [ ] **Step 2: Run admin test and confirm it fails**

Run: `cd tonepilot-admin/backend && mvn -Dtest=RuntimeIngestServiceTest test`
Expected: FAIL because runtime ingest classes do not exist.

- [ ] **Step 3: Implement schema, service, controller**

Add `runtime_user`、`runtime_device`、`runtime_event` tables. `RuntimeIngestService` uses `JdbcTemplate` to upsert device fingerprint, create the user concept on first registration, and append JSON event payloads.

- [ ] **Step 4: Run admin tests**

Run: `cd tonepilot-admin/backend && mvn test`
Expected: PASS.

### Task 2: Java Local Runtime 工程骨架

**Files:**
- Create: `clients/lightroom-classic/local-runtime/pom.xml`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/TonePilotLocalRuntimeApplication.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/config/RuntimeProperties.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/resources/application.yml`
- Test: `clients/lightroom-classic/local-runtime/src/test/java/com/tonepilot/runtime/config/RuntimePropertiesTest.java`

- [ ] **Step 1: Write failing runtime configuration test**

```java
@Test
void defaultPortAndBridgeRootAreLightroomCompatible() {
    RuntimeProperties properties = new RuntimeProperties();
    assertThat(properties.getBridge().getPort()).isEqualTo(33335);
    assertThat(properties.getBridge().getRoot()).contains(".tonepilot-lightroom-bridge");
}
```

- [ ] **Step 2: Run runtime test and confirm it fails**

Run: `cd clients/lightroom-classic/local-runtime && mvn -Dtest=RuntimePropertiesTest test`
Expected: FAIL because the Java project does not exist.

- [ ] **Step 3: Implement Spring Boot skeleton**

Create Maven project with `spring-boot-starter-web`、`spring-boot-starter-validation`、`lombok`、`spring-boot-starter-test` and `application.yml` using port `33335`.

- [ ] **Step 4: Run runtime skeleton test**

Run: `cd clients/lightroom-classic/local-runtime && mvn test`
Expected: PASS.

### Task 3: Java Agent 编排和 Lightroom 工具调用

**Files:**
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/bridge/BridgePaths.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/bridge/LightroomStateService.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/bridge/LightroomToolService.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/agent/RuleBasedRuntimeAgent.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/agent/RuntimeAgentOrchestrator.java`
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/api/LocalRuntimeController.java`
- Test: `clients/lightroom-classic/local-runtime/src/test/java/com/tonepilot/runtime/agent/RuleBasedRuntimeAgentTest.java`
- Test: `clients/lightroom-classic/local-runtime/src/test/java/com/tonepilot/runtime/bridge/BridgePathsTest.java`

- [ ] **Step 1: Write failing rule Agent tests**

```java
@Test
void doesNotChangeWhiteBalanceWhenPromptDoesNotMentionTemperature() {
    AgentTuneResult result = agent.plan(new AgentInput("夜景电影感，再亮一点", Map.of("Temperature", 4200)));
    assertThat(result.developSettings()).doesNotContainKeys("Temperature", "Tint");
}

@Test
void changesWhiteBalanceOnlyWhenPromptMentionsWarmth() {
    AgentTuneResult result = agent.plan(new AgentInput("整体暖一点", Map.of("Temperature", 4200)));
    assertThat(result.developSettings()).containsEntry("Temperature", 4500);
}
```

- [ ] **Step 2: Run agent tests and confirm they fail**

Run: `cd clients/lightroom-classic/local-runtime && mvn -Dtest=RuleBasedRuntimeAgentTest test`
Expected: FAIL because Agent classes do not exist.

- [ ] **Step 3: Implement rule Agent, selected-photo API, apply-job writer**

Rule Agent only outputs parameters directly implied by the user prompt. `LightroomToolService` writes Lua job files to `apply-jobs` and waits for `apply-results`.

- [ ] **Step 4: Run runtime tests**

Run: `cd clients/lightroom-classic/local-runtime && mvn test`
Expected: PASS.

### Task 4: 管理端同步和文档

**Files:**
- Create: `clients/lightroom-classic/local-runtime/src/main/java/com/tonepilot/runtime/admin/AdminRuntimeClient.java`
- Modify: `clients/lightroom-classic/local-runtime/README.md`
- Modify: `README.md`
- Modify: `clients/lightroom-classic/local-runtime/start-bridge-wsl.sh`
- Modify: `clients/lightroom-classic/local-runtime/start-bridge.ps1`

- [ ] **Step 1: Add sync tests**

```java
@Test
void adminSyncFailureDoesNotBreakLocalEditing() {
    AdminRuntimeClient client = AdminRuntimeClient.disabled();
    assertThatCode(() -> client.recordEvent("session.message", "session-1", Map.of("message", "test")))
            .doesNotThrowAnyException();
}
```

- [ ] **Step 2: Implement best-effort sync**

Local Runtime registers device on startup when `tonepilot.admin.base-url` is configured. Event sync failures are logged and do not block Lightroom editing.

- [ ] **Step 3: Update scripts and docs**

Scripts start Java runtime with Maven/JAR instead of Node. Docs describe `插件 -> 本地 Java Runtime -> 云端管理端 -> MySQL/Redis` deployment.

- [ ] **Step 4: Verify everything**

Run:
```bash
cd clients/lightroom-classic/local-runtime && mvn test
cd tonepilot-admin/backend && mvn test
git status --short
```

Expected: tests pass and only intended files are modified.
