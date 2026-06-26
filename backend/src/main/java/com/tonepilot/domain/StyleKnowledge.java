package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StyleKnowledge(
        Long id,
        Long styleId,
        Long sampleId,
        String title,
        String scene,
        String targetStyle,
        List<String> problems,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content,
        String embeddingId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
