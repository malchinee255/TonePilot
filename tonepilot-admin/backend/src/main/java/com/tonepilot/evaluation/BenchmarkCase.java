package com.tonepilot.evaluation;

import com.tonepilot.domain.PhotoAnalysis;

public record BenchmarkCase(
        String caseId,
        String name,
        String targetStyle,
        PhotoAnalysis analysis
) {
}
