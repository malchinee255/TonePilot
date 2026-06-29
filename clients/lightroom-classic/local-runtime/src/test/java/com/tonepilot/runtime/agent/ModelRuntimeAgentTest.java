package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

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
                Map.of("provider", "rule")
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
                Map.of("provider", "qwen2", "qwen2", Map.of("model", "qwen-plus"))
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("模型配置不完整");
    }
}
