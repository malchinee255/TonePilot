package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ModelRuntimeAgentTest {

    @Test
    void fallsBackToRuleAgentWhenProviderConfigIsMissing() {
        RuleBasedRuntimeAgent ruleAgent = new RuleBasedRuntimeAgent();
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();
        ReflectionTestUtils.setField(modelAgent, "traceLogger", mock(RuntimeTraceLogger.class));

        AgentTuneResult result = modelAgent.plan(
                new AgentInput("夜景电影感，再亮一点", Map.of("Temperature", 4200)),
                "qwen2",
                Map.of("provider", "qwen2", "qwen2", Map.of("model", "qwen-plus")),
                () -> ruleAgent.plan(new AgentInput("夜景电影感，再亮一点", Map.of("Temperature", 4200)))
        );

        assertThat(result.developSettings()).containsKeys("Exposure2012", "Shadows2012");
        assertThat(result.developSettings()).doesNotContainKeys("Temperature", "Tint");
    }
}
