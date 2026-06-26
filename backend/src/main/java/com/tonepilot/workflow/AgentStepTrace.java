package com.tonepilot.workflow;

import java.time.Instant;
import java.util.Map;

public record AgentStepTrace(
        String stepName,
        String agentName,
        AgentStepStatus status,
        Instant startedAt,
        Instant finishedAt,
        long latencyMs,
        int attempt,
        String inputSummary,
        String outputSummary,
        String errorMessage,
        Map<String, Object> metadata
) {
}
