package com.tonepilot.domain;

import java.time.Instant;

public record XmpExport(
        Long id,
        Long photoId,
        Long adjustmentId,
        String presetName,
        String xmpUrl,
        Instant createdAt
) {
}
