package com.tonepilot.runtime.agent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRuntimeAgentTest {

    @Test
    void fallsBackToRuleAgentWhenProviderConfigIsMissing() {
        RuleBasedRuntimeAgent ruleAgent = new RuleBasedRuntimeAgent();
        ModelRuntimeAgent modelAgent = new ModelRuntimeAgent();

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
