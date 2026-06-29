package com.tonepilot.observability;

import java.time.Instant;

public record LlmCallLog(
        String id,
        String runId,
        String provider,
        String modelName,
        String taskType,
        boolean success,
        long latencyMs,
        int promptChars,
        int responseChars,
        String promptPreview,
        String responsePreview,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
