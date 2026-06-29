package com.tonepilot.ai.dto;

import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;

import java.util.List;
import java.util.Map;

public record ColorAdjustmentModelOutput(
        String style,
        String reason,
        LightroomBasicParams basic,
        LightroomHslParams hsl,
        LightroomEffectsParams effects,
        Map<String, Object> extended,
        List<String> steps
) {
}
