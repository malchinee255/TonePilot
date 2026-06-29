package com.tonepilot.domain;

import java.time.Instant;

public record KnowledgeExtractionJob(
        Long id,
        Long sourceId,
        Long materialId,
        String status,
        Long generatedKnowledgeId,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
}
