package com.tonepilot.xmp;

import com.tonepilot.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XmpTemplateRendererTest {

    @Test
    void rendersCoreLightroomAttributes() {
        ColorAdjustment adjustment = new ColorAdjustment(
                1L,
                1L,
                "夜景电影感",
                "reason",
                new LightroomBasicParams(0.1, 12, -45, 28, -10, -18, -2, 3, 5, 8, 6, 10, -5),
                new LightroomHslParams(
                        0, -5, 0,
                        -2, 4, 10,
                        -5, -20, 0,
                        -10, -30, -5,
                        0, -10, 0,
                        -5, -12, -5,
                        0, 0, 0,
                        0, 0, 0
                ),
                new LightroomEffectsParams(10, -8),
                List.of("降低高光"),
                Map.of(),
                Instant.now()
        );

        String xmp = new XmpTemplateRenderer().render("Night Portrait Cinematic", adjustment);

        assertThat(xmp)
                .contains("crs:Exposure2012=\"0.10\"")
                .contains("crs:Highlights2012=\"-45\"")
                .contains("crs:SaturationAdjustmentGreen=\"-30\"")
                .contains("crs:PostCropVignetteAmount=\"-8\"");
    }
}
