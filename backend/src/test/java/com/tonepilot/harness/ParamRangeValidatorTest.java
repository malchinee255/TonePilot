package com.tonepilot.harness;

import com.tonepilot.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParamRangeValidatorTest {

    private final ParamRangeValidator validator = new ParamRangeValidator();

    @Test
    void reportsOutOfRangeExposure() {
        ColorAdjustment adjustment = adjustmentWithExposure(2.0);

        List<String> issues = validator.validate(adjustment);

        assertThat(issues).contains("exposure 超出 ±1.5");
    }

    @Test
    void acceptsConservativeExposure() {
        ColorAdjustment adjustment = adjustmentWithExposure(0.2);

        List<String> issues = validator.validate(adjustment);

        assertThat(issues).contains("exposure 在安全范围内");
    }

    private ColorAdjustment adjustmentWithExposure(double exposure) {
        return new ColorAdjustment(
                1L,
                1L,
                "夜景电影感",
                "reason",
                new LightroomBasicParams(exposure, 10, -40, 20, -8, -12, 0, 0, 4, 6, 4, 8, -4),
                new LightroomHslParams(
                        0, 0, 0,
                        0, 0, 10,
                        0, -10, 0,
                        0, -25, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0
                ),
                new LightroomEffectsParams(6, -8),
                List.of("step"),
                Map.of(),
                Instant.now()
        );
    }
}
