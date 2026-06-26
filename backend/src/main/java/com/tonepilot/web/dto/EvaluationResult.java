package com.tonepilot.web.dto;

import java.util.List;

public record EvaluationResult(
        boolean passed,
        int score,
        List<String> issues
) {
}
