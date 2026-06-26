package com.tonepilot.tuning;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.render.PreviewRender;

import java.time.Instant;
import java.util.List;

public record TuningSession(
        String id,
        Long photoId,
        Long sourceAdjustmentId,
        ColorAdjustment currentAdjustment,
        List<TuningMessage> messages,
        List<ParameterDelta> latestDeltas,
        PreviewRender preview,
        boolean saved,
        Instant createdAt,
        Instant updatedAt
) {
}
