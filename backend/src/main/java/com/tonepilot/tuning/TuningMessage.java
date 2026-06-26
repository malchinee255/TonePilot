package com.tonepilot.tuning;

import java.time.Instant;

public record TuningMessage(
        String role,
        String content,
        Instant createdAt
) {
}
