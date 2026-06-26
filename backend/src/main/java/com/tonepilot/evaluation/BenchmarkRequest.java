package com.tonepilot.evaluation;

import java.util.List;

public record BenchmarkRequest(
        List<String> providers
) {
}
