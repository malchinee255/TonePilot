package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ColorKnowledge(
        Long id,
        String title,
        String scene,
        List<String> problems,
        String targetStyle,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content,
        String embeddingId,
        Instant createdAt
) {
}
