package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeAgentOrchestratorTest {

    @Test
    void returnsImmediatelyWhenLightroomPluginHeartbeatIsMissing() {
        TestContext context = new TestContext();
        when(context.stateService.status()).thenReturn(Map.of(
                "available", false,
                "message", "TonePilot Local Runtime 已启动，但 Lightroom 插件尚未写入心跳。"
        ));

        Map<String, Object> result = context.orchestrator.chat(Map.of(
                "message", "夜景电影感",
                "provider", "qwen2",
                "sessionId", "session-1"
        ));

        assertThat(result).containsEntry("success", false);
        assertThat(String.valueOf(result.get("message"))).contains("Lightroom 插件不可用");
        verify(context.toolService, never()).applyDevelopSettings(anyMap());
        verify(context.traceLogger).warn(eq("agent.lightroom.unavailable"), eq("session-1"), anyMap());
    }

    @Test
    void analysisOnlyRequestRespondsWithoutApplyingLightroomSettings() {
        TestContext context = new TestContext();
        context.lightroomAvailable();
        when(context.configService.readInternalConfig()).thenReturn(Map.of("provider", "qwen2"));
        when(context.modelAgent.plan(any(), eq("qwen2"), anyMap(), any())).thenReturn(new AgentTuneResult(
                "这张照片是夜景城市照片，画面偏暗，灯光层次可以作为主要表现点。",
                Map.of(),
                List.of(),
                Map.of("intent", "分析照片", "photoType", "夜景城市照片", "recommendedStyle", "保留当前参数，先说明可调方向"),
                "{\"assistantMessage\":\"这张照片是夜景城市照片\"}"
        ));

        Map<String, Object> result = context.orchestrator.chat(Map.of(
                "message", "帮我分析下这张图",
                "provider", "qwen2",
                "sessionId", "session-2"
        ));

        assertThat(result).containsEntry("success", true);
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertThat(data).containsEntry("action", "respond");
        assertThat(data).containsKey("modelRawContent");
        verify(context.toolService, never()).applyDevelopSettings(anyMap());
    }

    @Test
    void explicitEditingRequestAppliesLightroomSettingsAutonomously() {
        TestContext context = new TestContext();
        context.lightroomAvailable();
        when(context.configService.readInternalConfig()).thenReturn(Map.of("provider", "qwen2"));
        when(context.modelAgent.plan(any(), eq("qwen2"), anyMap(), any())).thenReturn(new AgentTuneResult(
                "我会压一点高光、提亮暗部，并增强夜景电影感。",
                Map.of("Exposure2012", 0.2),
                List.of(new AgentDelta("basic", "Exposure2012", "曝光", 0, 0.2, 0.2, "提亮画面")),
                Map.of("intent", "修成夜景电影感", "photoType", "夜景", "recommendedStyle", "夜景电影感"),
                "{\"assistantMessage\":\"我会压一点高光、提亮暗部\"}"
        ));
        when(context.toolService.applyDevelopSettings(anyMap())).thenReturn(Map.of(
                "success", true,
                "pending", true,
                "jobId", "agent-apply-1",
                "message", "已提交 Lightroom 调色任务。"
        ));

        Map<String, Object> result = context.orchestrator.chat(Map.of(
                "message", "修成夜景电影感，再亮一点",
                "provider", "qwen2",
                "sessionId", "session-3"
        ));

        assertThat(result).containsEntry("success", true);
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertThat(data).containsEntry("action", "tool_submitted");
        assertThat((Map<String, Object>) data.get("apply")).containsEntry("jobId", "agent-apply-1");
        verify(context.toolService).applyDevelopSettings(Map.of("Exposure2012", 0.2));
    }

    private static class TestContext {
        private final LightroomStateService stateService = mock(LightroomStateService.class);
        private final LightroomToolService toolService = mock(LightroomToolService.class);
        private final RuleBasedRuntimeAgent ruleAgent = mock(RuleBasedRuntimeAgent.class);
        private final ModelRuntimeAgent modelAgent = mock(ModelRuntimeAgent.class);
        private final RuntimeConfigService configService = mock(RuntimeConfigService.class);
        private final AdminRuntimeClient adminRuntimeClient = mock(AdminRuntimeClient.class);
        private final RuntimeTraceLogger traceLogger = mock(RuntimeTraceLogger.class);
        private final RuntimeAgentOrchestrator orchestrator = new RuntimeAgentOrchestrator();

        TestContext() {
            ReflectionTestUtils.setField(orchestrator, "stateService", stateService);
            ReflectionTestUtils.setField(orchestrator, "toolService", toolService);
            ReflectionTestUtils.setField(orchestrator, "ruleAgent", ruleAgent);
            ReflectionTestUtils.setField(orchestrator, "modelAgent", modelAgent);
            ReflectionTestUtils.setField(orchestrator, "configService", configService);
            ReflectionTestUtils.setField(orchestrator, "adminRuntimeClient", adminRuntimeClient);
            ReflectionTestUtils.setField(orchestrator, "traceLogger", traceLogger);
        }

        void lightroomAvailable() {
            when(stateService.status()).thenReturn(Map.of("available", true));
            when(stateService.selectedPhoto()).thenReturn(Map.of(
                    "available", true,
                    "photo", Map.of("fileName", "DSCF0001.RAF"),
                    "currentAdjustment", Map.of()
            ));
        }
    }
}
