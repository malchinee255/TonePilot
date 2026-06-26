package com.tonepilot.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ColorAdjustment(
        Long id,
        Long photoId,
        String style,
        String reason,
        LightroomBasicParams basic,
        LightroomHslParams hsl,
        LightroomEffectsParams effects,
        List<String> steps,
        Map<String, Object> rawResponse,
        Instant createdAt
) {
}
