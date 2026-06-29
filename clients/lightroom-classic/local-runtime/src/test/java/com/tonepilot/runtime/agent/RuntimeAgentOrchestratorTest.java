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
    void firstChatReturnsAgentDecisionWithoutApplyingLightroomSettings() {
        TestContext context = new TestContext();
        context.lightroomAvailable();
        when(context.configService.readInternalConfig()).thenReturn(Map.of("provider", "rule"));
        when(context.modelAgent.plan(any(), eq("rule"), anyMap(), any())).thenReturn(new AgentTuneResult(
                "建议走夜景电影感。你确认后我再调用 Lightroom。",
                Map.of("Exposure2012", 0.2),
                List.of(new AgentDelta("basic", "Exposure2012", "曝光", 0, 0.2, 0.2, "提亮画面")),
                Map.of("intent", "夜景电影感", "photoType", "夜景", "recommendedStyle", "夜景电影感"),
                "{\"assistantMessage\":\"建议走夜景电影感\"}"
        ));

        Map<String, Object> result = context.orchestrator.chat(Map.of(
                "message", "先分析这张照片，修成夜景电影感",
                "provider", "rule",
                "sessionId", "session-2"
        ));

        assertThat(result).containsEntry("success", true);
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertThat(data).containsEntry("action", "await_user_decision");
        assertThat(data).containsKey("developSettings");
        assertThat(data).containsKey("modelRawContent");
        verify(context.toolService, never()).applyDevelopSettings(anyMap());
    }

    @Test
    void confirmationChatAppliesLastAgentDecisionAsAsyncToolTask() {
        TestContext context = new TestContext();
        context.lightroomAvailable();
        when(context.configService.readInternalConfig()).thenReturn(Map.of("provider", "rule"));
        when(context.modelAgent.plan(any(), eq("rule"), anyMap(), any())).thenReturn(new AgentTuneResult(
                "建议走夜景电影感。你确认后我再调用 Lightroom。",
                Map.of("Exposure2012", 0.2),
                List.of(new AgentDelta("basic", "Exposure2012", "曝光", 0, 0.2, 0.2, "提亮画面")),
                Map.of("intent", "夜景电影感", "photoType", "夜景", "recommendedStyle", "夜景电影感"),
                "{\"assistantMessage\":\"建议走夜景电影感\"}"
        ));
        when(context.toolService.applyDevelopSettings(anyMap())).thenReturn(Map.of(
                "success", true,
                "pending", true,
                "jobId", "agent-apply-1",
                "message", "已提交 Lightroom 调色任务。"
        ));

        context.orchestrator.chat(Map.of(
                "message", "先分析这张照片，修成夜景电影感",
                "provider", "rule",
                "sessionId", "session-3"
        ));
        Map<String, Object> result = context.orchestrator.chat(Map.of(
                "message", "确认，按这个修",
                "provider", "rule",
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
        private final AgentConversationMemory memory = new AgentConversationMemory();
        private final RuntimeAgentOrchestrator orchestrator = new RuntimeAgentOrchestrator();

        TestContext() {
            ReflectionTestUtils.setField(orchestrator, "stateService", stateService);
            ReflectionTestUtils.setField(orchestrator, "toolService", toolService);
            ReflectionTestUtils.setField(orchestrator, "ruleAgent", ruleAgent);
            ReflectionTestUtils.setField(orchestrator, "modelAgent", modelAgent);
            ReflectionTestUtils.setField(orchestrator, "configService", configService);
            ReflectionTestUtils.setField(orchestrator, "adminRuntimeClient", adminRuntimeClient);
            ReflectionTestUtils.setField(orchestrator, "traceLogger", traceLogger);
            ReflectionTestUtils.setField(orchestrator, "memory", memory);
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
