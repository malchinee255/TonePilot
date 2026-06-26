package com.tonepilot.ai.dto;

import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;

import java.util.List;

public record ColorAdjustmentModelOutput(
        String style,
        String reason,
        LightroomBasicParams basic,
        LightroomHslParams hsl,
        LightroomEffectsParams effects,
        List<String> steps
) {
}
