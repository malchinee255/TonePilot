package com.tonepilot.runtime.agent;

import java.util.List;
import java.util.Map;

public record AgentTuneResult(
        String assistantMessage,
        AgentThought agentThought,
        Map<String, Object> developSettings,
        List<AgentDelta> deltas,
        Map<String, Object> analysis,
        List<Map<String, Object>> localAdjustments,
        String rawModelContent
) {
    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, List.of(), "");
    }

    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis,
            String rawModelContent
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, List.of(), rawModelContent);
    }

    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis,
            List<Map<String, Object>> localAdjustments,
            String rawModelContent
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, localAdjustments, rawModelContent);
    }
}
