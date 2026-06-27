package com.tonepilot.lightroomagent;

import com.tonepilot.agent.RuleBasedParamValidationAgent;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.harness.ParamRangeValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LightroomAgentServiceTest {

    private final LightroomAgentService service = new LightroomAgentService(
            new TuningAdjustmentPlanner(new RuleBasedParamValidationAgent(new ParamRangeValidator())),
            new LightroomDevelopSettingsMapper()
    );

    @Test
    void tunesFromLightroomPromptAndReturnsDevelopSettings() {
        LightroomAgentTuneResponse response = service.tune(new LightroomAgentTuneRequest(
                null,
                "DSCF1719.RAF",
                "调成夜景电影感，再亮一点",
                "rule",
                null
        ));

        assertThat(response.sessionId()).isNotBlank();
        assertThat(response.localPhotoId()).isEqualTo("DSCF1719.RAF");
        assertThat(response.adjustment().basic().exposure()).isGreaterThan(0);
        assertThat(response.deltas()).extracting("name").contains("exposure", "contrast", "dehaze");
        assertThat(response.developSettings())
                .containsEntry("Exposure2012", response.adjustment().basic().exposure())
                .containsEntry("Contrast2012", response.adjustment().basic().contrast())
                .containsEntry("Dehaze", response.adjustment().basic().dehaze());
    }

    @Test
    void acceptsPartialCurrentAdjustmentFromLightroomPlugin() {
        ColorAdjustment partialCurrentAdjustment = new ColorAdjustment(
                null,
                null,
                "Lightroom 当前参数",
                "插件只读取到基础面板参数",
                new LightroomBasicParams(0.1, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                null,
                null,
                null,
                null,
                null
        );

        LightroomAgentTuneResponse response = service.tune(new LightroomAgentTuneRequest(
                "manual-check",
                "DSCF1715.RAF",
                "夜景电影感，再亮一点",
                "rule",
                partialCurrentAdjustment
        ));

        assertThat(response.sessionId()).isEqualTo("manual-check");
        assertThat(response.localPhotoId()).isEqualTo("DSCF1715.RAF");
        assertThat(response.adjustment().hsl()).isNotNull();
        assertThat(response.adjustment().effects()).isNotNull();
        assertThat(response.developSettings()).containsKeys("Exposure2012", "Contrast2012", "Dehaze");
    }
}
