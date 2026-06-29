package com.tonepilot.runtime.agent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedRuntimeAgentTest {

    private final RuleBasedRuntimeAgent agent = new RuleBasedRuntimeAgent();

    @Test
    void doesNotChangeWhiteBalanceWhenPromptDoesNotMentionTemperature() {
        AgentTuneResult result = agent.plan(new AgentInput("夜景电影感，再亮一点", Map.of("Temperature", 4200)));

        assertThat(result.developSettings()).doesNotContainKeys("Temperature", "Tint");
    }

    @Test
    void changesWhiteBalanceOnlyWhenPromptMentionsWarmth() {
        AgentTuneResult result = agent.plan(new AgentInput("整体暖一点", Map.of("Temperature", 4200)));

        assertThat(result.developSettings()).containsEntry("Temperature", 4500.0);
    }
}
