package com.tonepilot.domain;

import java.time.Instant;

public record KnowledgeMaterial(
        Long id,
        Long sourceId,
        String materialType,
        String title,
        String content,
        String language,
        Instant createdAt
) {
}
