package com.tonepilot.runtime;

import java.time.Instant;

public record RuntimeEventRecord(
        String id,
        String userId,
        String deviceId,
        String eventType,
        String sessionId,
        String payloadJson,
        Instant createdAt
) {
}
