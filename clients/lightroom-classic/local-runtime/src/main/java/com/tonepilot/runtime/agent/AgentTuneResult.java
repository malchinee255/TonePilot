package com.tonepilot.runtime.agent;

import java.util.List;
import java.util.Map;

public record AgentTuneResult(
        String assistantMessage,
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
        this(assistantMessage, developSettings, deltas, analysis, List.of(), "");
    }

    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis,
            String rawModelContent
    ) {
        this(assistantMessage, developSettings, deltas, analysis, List.of(), rawModelContent);
    }
}
