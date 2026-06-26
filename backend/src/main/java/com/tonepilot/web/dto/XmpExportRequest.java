package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record XmpExportRequest(
        @NotNull Long photoId,
        @NotNull Long adjustmentId,
        @NotBlank String presetName
) {
}
