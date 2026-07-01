package com.tonepilot.starter.agent;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.config.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.lightroom.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.starter.*;





import com.tonepilot.infrastructure.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ModelRuntimeAgentTest {

    @Test
    void rejectsRemovedRuleProvider() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();
        ReflectionTestUtils.setField(modelAgent, "traceLogger", mock(RuntimeTraceLogger.class));

        assertThatThrownBy(() -> modelAgent.plan(
                new AgentInput("帮我修图", Map.of()),
                "rule",
                Map.of("provider", "rule"),
                "session-test"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本地规则模式已移除");
    }

    @Test
    void rejectsIncompleteProviderConfigWithoutRuleFallback() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();
        ReflectionTestUtils.setField(modelAgent, "traceLogger", mock(RuntimeTraceLogger.class));

        assertThatThrownBy(() -> modelAgent.plan(
                new AgentInput("帮我修图", Map.of("Temperature", 4200)),
                "qwen2",
                Map.of("provider", "qwen2", "qwen2", Map.of("model", "qwen-plus")),
                "session-test"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("模型配置不完整");
    }

    @Test
    void qwenRequestDisablesThinkingAndLimitsOutputTokens() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(
                modelAgent,
                "buildChatRequestPayload",
                "qwen2",
                "qwen3.6-plus",
                "system prompt",
                "user prompt"
        );

        assertThat(payload).containsEntry("enable_thinking", false);
        assertThat(payload).containsEntry("max_tokens", 1600);
    }

    @Test
    void userPromptIncludesPhotoMetadataAndPreviewReference() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();

        String prompt = ReflectionTestUtils.invokeMethod(
                modelAgent,
                "userPrompt",
                new AgentInput(
                        "帮我优化照片",
                        Map.of("Temperature", 4200),
                        java.util.List.of(),
                        Map.of("fileName", "DSCF1709.RAF", "camera", "Fujifilm GFX100RF"),
                        "/files/selected-preview.jpg?t=1782890000"
                )
        );

        assertThat(prompt).contains("用户意图");
        assertThat(prompt).contains("当前照片元数据");
        assertThat(prompt).contains("DSCF1709.RAF");
        assertThat(prompt).contains("当前照片预览");
    }

    @Test
    void visionModelBuildsMultimodalMessageWithImageUrl() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(
                modelAgent,
                "buildChatRequestPayload",
                "qwen2",
                "qwen-vl-plus",
                "system prompt",
                "user prompt",
                "data:image/jpeg;base64,abc"
        );

        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) payload.get("messages");
        Object userContent = messages.get(1).get("content");
        assertThat(userContent).isInstanceOf(java.util.List.class);
        assertThat(String.valueOf(userContent)).contains("image_url");
        assertThat(String.valueOf(userContent)).contains("data:image/jpeg;base64,abc");
    }


    @Test
    void parsesMainAgentThoughtFromModelResult() {
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();
        ReflectionTestUtils.setField(modelAgent, "traceLogger", mock(RuntimeTraceLogger.class));
        String modelResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"assistantMessage\\":\\"我先判断这是一张夜景城市照片。\\",\\"agentThought\\":{\\"summary\\":\\"画面偏暗但灯光层次完整\\",\\"observations\\":[\\"天空和水面占比较高\\",\\"建筑灯光是视觉中心\\"],\\"reasoningVisible\\":\\"这类照片适合压住高光、提亮暗部并保留夜景氛围。\\",\\"decision\\":\\"apply_global_adjustments\\",\\"nextAction\\":\\"调用 Lightroom 全局调色工具\\",\\"toolPlan\\":[\\"提高曝光\\",\\"压低高光\\"],\\"userOptions\\":[\\"确认按这个修\\",\\"先不要修，只看分析\\"]},\\"analysis\\":{\\"intent\\":\\"夜景电影感\\",\\"photoType\\":\\"城市夜景\\",\\"recommendedStyle\\":\\"冷暖对比电影感\\"},\\"developSettings\\":{\\"Exposure2012\\":0.18},\\"localAdjustments\\":[]}"
                      }
                    }
                  ]
                }
                """;

        AgentTuneResult result = ReflectionTestUtils.invokeMethod(
                modelAgent,
                "parseModelResult",
                new AgentInput("帮我分析并修图", Map.of("Exposure2012", 0)),
                modelResponse,
                "session-test"
        );

        assertThat(result).isNotNull();
        assertThat(result.agentThought()).isNotNull();
        assertThat(result.agentThought().summary()).isEqualTo("画面偏暗但灯光层次完整");
        assertThat(result.agentThought().observations()).contains("天空和水面占比较高");
        assertThat(result.agentThought().decision()).isEqualTo("apply_global_adjustments");
        assertThat(result.agentThought().userOptions()).contains("确认按这个修");
    }
}
