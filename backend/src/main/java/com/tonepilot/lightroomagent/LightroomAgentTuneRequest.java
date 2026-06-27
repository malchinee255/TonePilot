package com.tonepilot.lightroomagent;

import com.tonepilot.domain.ColorAdjustment;
import jakarta.validation.constraints.NotBlank;

public record LightroomAgentTuneRequest(
        String sessionId,
        String localPhotoId,
        @NotBlank String message,
        String provider,
        ColorAdjustment currentAdjustment
) {
}
