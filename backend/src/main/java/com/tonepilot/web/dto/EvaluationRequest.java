package com.tonepilot.web.dto;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;

public record EvaluationRequest(
        Long photoId,
        Long adjustmentId,
        PhotoAnalysis analysis,
        ColorAdjustment adjustment
) {
}
