package com.tonepilot.tuning;

import com.tonepilot.agent.RuleBasedParamValidationAgent;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import com.tonepilot.harness.ParamRangeValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TuningAdjustmentPlannerTest {

    private final TuningAdjustmentPlanner planner = new TuningAdjustmentPlanner(
            new RuleBasedParamValidationAgent(new ParamRangeValidator())
    );

    @Test
    void appliesMultiTurnBrightnessAndWarmthIntent() {
        ColorAdjustment source = neutralAdjustment();

        TuningPlan plan = planner.apply(source, "再亮一点，整体暖一点");

        assertThat(plan.adjustment().basic().exposure()).isGreaterThan(source.basic().exposure());
        assertThat(plan.adjustment().basic().temperature()).isGreaterThan(source.basic().temperature());
        assertThat(plan.deltas())
                .extracting(ParameterDelta::name)
                .contains("exposure", "temperature");
    }

    @Test
    void reportsNoChangeWhenIntentIsNotRecognized() {
        ColorAdjustment source = neutralAdjustment();

        TuningPlan plan = planner.apply(source, "我想要更有感觉一些");

        assertThat(plan.adjustment()).isEqualTo(source);
        assertThat(plan.deltas()).isEmpty();
        assertThat(plan.assistantMessage()).contains("没有识别到明确的调色动作");
    }

    private ColorAdjustment neutralAdjustment() {
        return new ColorAdjustment(
                1L,
                9L,
                "自然纪实",
                "初始参数",
                new LightroomBasicParams(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new LightroomHslParams(
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0
                ),
                new LightroomEffectsParams(0, 0),
                List.of("保留原始影调"),
                Map.of(),
                Instant.now()
        );
    }
}
