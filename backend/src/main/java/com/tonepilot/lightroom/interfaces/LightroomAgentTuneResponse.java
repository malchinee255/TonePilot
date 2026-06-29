package com.tonepilot.lightroom.interfaces;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.lightroom.domain.ParameterDelta;

import java.util.List;
import java.util.Map;

public record LightroomAgentTuneResponse(
        String sessionId,
        String localPhotoId,
        ColorAdjustment adjustment,
        List<ParameterDelta> deltas,
        String assistantMessage,
        Map<String, Object> developSettings
) {
}
