package com.tonepilot.observability;

import java.time.Instant;

public record AuditEvent(
        String id,
        String eventType,
        String actor,
        String runId,
        String targetType,
        String targetId,
        String detail,
        Instant createdAt
) {
}
