package com.tonepilot.domain.colorgrading;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







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
        Map<String, Object> extended,
        List<String> steps,
        Map<String, Object> rawResponse,
        Instant createdAt
) {
    public ColorAdjustment(
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
        this(id, photoId, style, reason, basic, hsl, effects, Map.of(), steps, rawResponse, createdAt);
    }

    public ColorAdjustment {
        extended = extended == null ? Map.of() : extended;
    }
}
