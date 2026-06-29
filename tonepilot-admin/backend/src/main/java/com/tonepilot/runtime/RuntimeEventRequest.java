package com.tonepilot.runtime;

import java.util.Map;

public record RuntimeEventRequest(
        String userId,
        String deviceId,
        String eventType,
        String sessionId,
        Map<String, Object> payload
) {
}
