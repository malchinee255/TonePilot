package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DouyinImportRequest(
        @NotBlank String videoUrl,
        @NotBlank String title,
        String author,
        Long styleId,
        String notes
) {
}
