package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;

public record ColorStyle(
        Long id,
        String styleName,
        String styleCode,
        String description,
        List<String> suitableScenes,
        List<String> avoidScenes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
