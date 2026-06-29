package com.tonepilot.lightroom.domain;

import com.tonepilot.colorgrading.domain.ColorAdjustment;

import java.util.List;

public record TuningPlan(
        ColorAdjustment adjustment,
        List<ParameterDelta> deltas,
        String assistantMessage
) {
}
