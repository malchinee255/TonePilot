package com.tonepilot.lightroomagent;

import com.tonepilot.domain.ColorAdjustment;

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
