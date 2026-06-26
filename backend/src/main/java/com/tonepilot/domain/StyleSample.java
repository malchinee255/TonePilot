package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;

public record StyleSample(
        Long id,
        Long styleId,
        String sampleType,
        String beforeImageUrl,
        String afterImageUrl,
        String finalImageUrl,
        String sourceType,
        String description,
        List<String> tags,
        StyleAnalysisResult analysisResult,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
