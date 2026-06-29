package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record StyleRequest(
        @NotBlank String styleName,
        @NotBlank String styleCode,
        String description,
        List<String> suitableScenes,
        List<String> avoidScenes,
        String status
) {
}
