package com.tonepilot.lightroom.interfaces;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import jakarta.validation.constraints.NotBlank;

public record LightroomAgentTuneRequest(
        String sessionId,
        String localPhotoId,
        @NotBlank String message,
        String provider,
        ColorAdjustment currentAdjustment
) {
}
