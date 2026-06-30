package com.tonepilot.harness;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;




import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.domain.colorgrading.LightroomBasicParams;
import com.tonepilot.domain.colorgrading.LightroomEffectsParams;
import com.tonepilot.domain.colorgrading.LightroomHslParams;
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
