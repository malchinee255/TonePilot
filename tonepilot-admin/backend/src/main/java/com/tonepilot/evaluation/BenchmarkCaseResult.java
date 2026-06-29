package com.tonepilot.evaluation;

import java.util.List;

public record BenchmarkCaseResult(
        String caseId,
        String name,
        String targetStyle,
        boolean passed,
        int score,
        String generatedStyle,
        List<String> issues
) {
}
