package com.tonepilot.evaluation;

import java.util.List;

public record BenchmarkProviderResult(
        String provider,
        int caseCount,
        int passedCount,
        double passRate,
        double averageScore,
        List<BenchmarkCaseResult> cases
) {
}
