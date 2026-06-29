package com.tonepilot.runtime;

public record RuntimeDeviceRegistrationResponse(
        String userId,
        String deviceId,
        boolean created
) {
}
