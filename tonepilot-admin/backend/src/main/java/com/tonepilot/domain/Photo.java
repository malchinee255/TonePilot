package com.tonepilot.domain;

import java.time.Instant;

public record Photo(
        Long id,
        String fileName,
        String fileUrl,
        String fileType,
        Instant uploadedAt
) {
}
