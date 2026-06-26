package com.tonepilot.tuning;

public record TuningMessageRequest(
        String message,
        String provider
) {
}
