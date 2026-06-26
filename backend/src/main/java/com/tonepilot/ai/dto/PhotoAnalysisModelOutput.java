package com.tonepilot.ai.dto;

import java.util.List;

public record PhotoAnalysisModelOutput(
        String scene,
        String subject,
        List<String> exposureIssues,
        List<String> whiteBalanceIssues,
        List<String> colorIssues,
        List<String> recommendedStyles,
        String summary
) {
}
