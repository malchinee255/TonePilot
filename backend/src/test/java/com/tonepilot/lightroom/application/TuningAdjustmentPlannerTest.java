package com.tonepilot.lightroom.application;

import com.tonepilot.agent.RuleBasedParamValidationAgent;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;
import com.tonepilot.harness.ParamRangeValidator;
import com.tonepilot.lightroom.domain.ParameterDelta;
import com.tonepilot.lightroom.domain.TuningPlan;
import com.tonepilot.lightroom.infrastructure.LightroomDevelopSettingsMapper;
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
    void doesNotChangeWhiteBalanceWhenPromptOnlyAsksForCinematicBrightness() {
        ColorAdjustment source = neutralAdjustment();

        TuningPlan plan = planner.apply(source, "调成夜景电影感，再亮一点");

        assertThat(plan.adjustment().basic().temperature()).isEqualTo(source.basic().temperature());
        assertThat(plan.adjustment().basic().tint()).isEqualTo(source.basic().tint());
        assertThat(plan.deltas())
                .extracting(ParameterDelta::name)
                .doesNotContain("temperature", "tint");
    }

    @Test
    void reportsNoChangeWhenIntentIsNotRecognized() {
        ColorAdjustment source = neutralAdjustment();

        TuningPlan plan = planner.apply(source, "我想要更有感觉一点");

        assertThat(plan.adjustment()).isEqualTo(source);
        assertThat(plan.deltas()).isEmpty();
        assertThat(plan.assistantMessage()).contains("没有识别到明确的调色动作");
    }

    @Test
    void keepsExtendedDeltasWhenParameterDidNotExistBefore() {
        ColorAdjustment source = neutralAdjustment();

        TuningPlan plan = planner.apply(source, "锐化一点，保留更多细节");
        Map<String, Object> settings = new LightroomDevelopSettingsMapper()
                .toDevelopSettings(plan.adjustment(), plan.deltas());

        assertThat(plan.deltas())
                .extracting(ParameterDelta::name)
                .contains("sharpness", "sharpenDetail");
        assertThat(settings)
                .containsEntry("Sharpness", 45)
                .containsEntry("SharpenDetail", 25)
                .doesNotContainKeys("Temperature", "Tint");
    }

    private ColorAdjustment neutralAdjustment() {
        return new ColorAdjustment(
                1L,
                9L,
                "自然纪实",
                "初始参数",
                new LightroomBasicParams(0, 0, 0, 0, 0, 0, 4200, 17, 0, 0, 0, 0, 0),
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
