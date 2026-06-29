package com.tonepilot.runtime.agent;

import java.util.List;
import java.util.Map;

public record AgentTuneResult(
        String assistantMessage,
        Map<String, Object> developSettings,
        List<AgentDelta> deltas,
        Map<String, Object> analysis,
        String rawModelContent
) {
    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis
    ) {
        this(assistantMessage, developSettings, deltas, analysis, "");
    }
}
