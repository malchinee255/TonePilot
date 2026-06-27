package com.tonepilot.lightroomagent;

import com.tonepilot.domain.ColorAdjustment;

import java.util.List;

public record TuningPlan(
        ColorAdjustment adjustment,
        List<ParameterDelta> deltas,
        String assistantMessage
) {
}
