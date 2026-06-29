package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeAgentOrchestratorTest {

    @Test
    void returnsImmediatelyWhenLightroomPluginHeartbeatIsMissing() {
        LightroomStateService stateService = mock(LightroomStateService.class);
        LightroomToolService toolService = mock(LightroomToolService.class);
        RuleBasedRuntimeAgent ruleAgent = mock(RuleBasedRuntimeAgent.class);
        ModelRuntimeAgent modelAgent = mock(ModelRuntimeAgent.class);
        RuntimeConfigService configService = mock(RuntimeConfigService.class);
        AdminRuntimeClient adminRuntimeClient = mock(AdminRuntimeClient.class);
        RuntimeTraceLogger traceLogger = mock(RuntimeTraceLogger.class);
        when(stateService.status()).thenReturn(Map.of(
                "available", false,
                "message", "TonePilot Local Runtime 已启动，但 Lightroom 插件尚未写入心跳。"
        ));

        RuntimeAgentOrchestrator orchestrator = new RuntimeAgentOrchestrator();
        ReflectionTestUtils.setField(orchestrator, "stateService", stateService);
        ReflectionTestUtils.setField(orchestrator, "toolService", toolService);
        ReflectionTestUtils.setField(orchestrator, "ruleAgent", ruleAgent);
        ReflectionTestUtils.setField(orchestrator, "modelAgent", modelAgent);
        ReflectionTestUtils.setField(orchestrator, "configService", configService);
        ReflectionTestUtils.setField(orchestrator, "adminRuntimeClient", adminRuntimeClient);
        ReflectionTestUtils.setField(orchestrator, "traceLogger", traceLogger);

        Map<String, Object> result = orchestrator.chat(Map.of(
                "message", "夜景电影感",
                "provider", "qwen2",
                "sessionId", "session-1"
        ));

        assertThat(result).containsEntry("success", false);
        assertThat(String.valueOf(result.get("message"))).contains("Lightroom 插件不可用");
        verify(toolService, never()).applyDevelopSettings(org.mockito.ArgumentMatchers.anyMap());
        verify(traceLogger).warn(
                org.mockito.ArgumentMatchers.eq("agent.lightroom.unavailable"),
                org.mockito.ArgumentMatchers.eq("session-1"),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }
}
