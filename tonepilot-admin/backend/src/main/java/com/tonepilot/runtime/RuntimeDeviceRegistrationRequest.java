package com.tonepilot.runtime;

import java.util.Map;

public record RuntimeDeviceRegistrationRequest(
        String fingerprint,
        String deviceName,
        String endpoint,
        Map<String, Object> metadata
) {
}
