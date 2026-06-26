package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdjustmentGenerateRequest(
        @NotNull Long photoId,
        String targetStyle,
        List<Long> knowledgeIds,
        String provider
) {
}
