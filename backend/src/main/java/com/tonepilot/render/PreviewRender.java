package com.tonepilot.render;

import java.time.Instant;

public record PreviewRender(
        String originalUrl,
        String previewUrl,
        Instant renderedAt
) {
}
