package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeMaterialRequest(
        @NotBlank String materialType,
        @NotBlank String title,
        @NotBlank String content,
        String language
) {
}
