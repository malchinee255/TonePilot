package com.tonepilot.domain.runtime;

public record RuntimeEventQuery(
        String userId,
        String sessionId,
        String traceId,
        String eventType,
        Integer limit
) {
    public int normalizedLimit() {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }
}
