package com.tonepilot.domain.runtime;

import java.time.Instant;

public record RuntimeDeviceRecord(
        String userId,
        String deviceId,
        String fingerprint,
        String deviceName,
        String endpoint,
        String metadataJson,
        Instant lastSeenAt,
        Instant createdAt
) {
}
