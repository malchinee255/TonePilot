package com.tonepilot.web.dto;

public record RagSearchItem(
        String sourceType,
        Long sourceId,
        String title,
        double score,
        String content
) {
}
