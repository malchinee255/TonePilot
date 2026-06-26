package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PhotoAnalysis(
        Long id,
        Long photoId,
        String scene,
        String subject,
        List<String> exposureIssues,
        List<String> whiteBalanceIssues,
        List<String> colorIssues,
        List<String> recommendedStyles,
        String summary,
        Map<String, Object> rawResponse,
        Instant createdAt
) {
}
