package com.tonepilot.domain;

import java.time.Instant;

public record KnowledgeSource(
        Long id,
        String sourceType,
        String title,
        String author,
        String originalUrl,
        Long styleId,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
