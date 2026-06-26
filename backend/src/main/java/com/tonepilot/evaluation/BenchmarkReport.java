package com.tonepilot.evaluation;

import java.time.Instant;
import java.util.List;

public record BenchmarkReport(
        String runId,
        Instant createdAt,
        List<BenchmarkProviderResult> providers
) {
}
