package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeSourceRequest(
        @NotBlank String sourceType,
        @NotBlank String title,
        String author,
        String originalUrl,
        Long styleId,
        String notes
) {
}
