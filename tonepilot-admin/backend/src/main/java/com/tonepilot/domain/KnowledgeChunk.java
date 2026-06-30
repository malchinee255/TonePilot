package com.tonepilot.domain;

import java.time.Instant;
import java.util.Map;

public record KnowledgeChunk(
        Long id,
        String sourceType,
        Long sourceId,
        String title,
        String content,
        Map<String, Double> embedding,
        Instant createdAt
) {
}
