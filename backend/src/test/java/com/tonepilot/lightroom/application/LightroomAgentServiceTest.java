package com.tonepilot.lightroom.application;

import com.tonepilot.agent.RuleBasedParamValidationAgent;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.harness.ParamRangeValidator;
import com.tonepilot.lightroom.domain.ParameterDelta;
import com.tonepilot.lightroom.infrastructure.LightroomDevelopSettingsMapper;
import com.tonepilot.lightroom.interfaces.LightroomAgentTuneRequest;
import com.tonepilot.lightroom.interfaces.LightroomAgentTuneResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        assertThat(response.developSettings()).doesNotContainKeys("Temperature", "Tint");
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

    @Test
    void mapsExtendedLightroomDevelopSettingsOnlyWhenRequestedByDelta() {
        LightroomDevelopSettingsMapper mapper = new LightroomDevelopSettingsMapper();
        ColorAdjustment adjustment = new ColorAdjustment(
                null,
                null,
                "Lightroom 扩展参数",
                "覆盖 Lightroom 更多 Develop Settings",
                new LightroomBasicParams(0, 0, 0, 0, 0, 0, 4200, 0, 0, 0, 0, 0, 0),
                null,
                null,
                Map.of(
                        "parametricHighlightSplit", 72,
                        "sharpness", 45,
                        "colorGradeMidtoneHue", 220,
                        "lensProfileEnable", 1,
                        "uprightTransformMode", 2
                ),
                null,
                null,
                null
        );

        Map<String, Object> settings = mapper.toDevelopSettings(adjustment, List.of(
                new ParameterDelta("toneCurve", "parametricHighlightSplit", "高光拆分", "50", "72", "+22", "调整参数曲线高光区域"),
                new ParameterDelta("detail", "sharpness", "锐化数量", "0", "45", "+45", "增强照片细节"),
                new ParameterDelta("colorGrading", "colorGradeMidtoneHue", "中间调色相", "0", "220", "+220", "设置颜色分级中间调"),
                new ParameterDelta("lensCorrections", "lensProfileEnable", "启用配置文件校正", "0", "1", "+1", "启用镜头校正"),
                new ParameterDelta("transform", "uprightTransformMode", "Upright 模式", "0", "2", "+2", "自动校正透视")
        ));

        assertThat(settings)
                .containsEntry("ParametricHighlightSplit", 72)
                .containsEntry("Sharpness", 45)
                .containsEntry("ColorGradeMidtoneHue", 220)
                .containsEntry("LensProfileEnable", 1)
                .containsEntry("UprightTransformMode", 2);
        assertThat(settings).doesNotContainKeys("Temperature", "Tint");
    }
}
