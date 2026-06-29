package com.tonepilot.agent;

import com.tonepilot.ai.AiProperties;
import com.tonepilot.ai.OpenAiCompatibleModelClient;
import com.tonepilot.ai.dto.ColorAdjustmentModelOutput;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class ModelBackedColorPlanningAgent implements ColorPlanningAgent {

    private final AiProperties properties;
    private final OpenAiCompatibleModelClient modelClient;
    private final RuleBasedColorPlanningAgent fallback;

    @Autowired
    public ModelBackedColorPlanningAgent(
            AiProperties properties,
            OpenAiCompatibleModelClient modelClient,
            RuleBasedColorPlanningAgent fallback
    ) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.fallback = fallback;
    }

    @Override
    public ColorAdjustment plan(Long photoId, String targetStyle, PhotoAnalysis analysis, List<RagSearchItem> knowledgeItems) {
        if (!properties.modelEnabled()) {
            return fallback.plan(photoId, targetStyle, analysis, knowledgeItems);
        }
        try {
            ColorAdjustment fallbackAdjustment = fallback.plan(photoId, targetStyle, analysis, knowledgeItems);
            String prompt = """
                    请根据照片分析结果、RAG 知识和目标风格生成 Lightroom 参数 JSON。
                    目标风格：%s
                    照片分析：%s
                    RAG 知识：%s
                    只输出 JSON，字段必须为 style, reason, basic, hsl, effects, extended, steps。
                    basic/hsl/effects 使用已有字段；extended 用于 Lightroom 其他 Develop Settings，例如：
                    parametricShadows, parametricDarks, parametricLights, parametricHighlights,
                    parametricShadowSplit, parametricMidtoneSplit, parametricHighlightSplit,
                    sharpness, sharpenRadius, sharpenDetail, sharpenEdgeMasking,
                    luminanceSmoothing, luminanceNoiseReductionDetail, luminanceNoiseReductionContrast,
                    colorNoiseReduction, colorNoiseReductionDetail, colorNoiseReductionSmoothness,
                    colorGradeShadowHue, colorGradeShadowSat, colorGradeMidtoneHue, colorGradeMidtoneSat,
                    colorGradeHighlightHue, colorGradeHighlightSat, colorGradeBlending,
                    lensProfileEnable, lensManualDistortionAmount, autoLateralCA,
                    uprightTransformMode, perspectiveVertical, perspectiveHorizontal, perspectiveRotate,
                    perspectiveScale, perspectiveAspect, perspectiveX, perspectiveY,
                    postCropVignetteStyle, postCropVignetteMidpoint, postCropVignetteRoundness,
                    postCropVignetteFeather, postCropVignetteHighlightContrast,
                    grainSize, grainFrequency,
                    redPrimaryHue, redPrimarySaturation, greenPrimaryHue, greenPrimarySaturation,
                    bluePrimaryHue, bluePrimarySaturation, shadowTint。
                    只填写本次风格确实需要的 extended 字段；不要为了凑字段而改白平衡、裁剪、镜头或透视。
                    参数必须保守，exposure 建议限制在 -1.5 到 1.5，其它常规滑块限制在 -100 到 100 内。
                    """.formatted(targetStyle, modelClient.writeJson(analysis), modelClient.writeJson(knowledgeItems));
            String json = modelClient.completeJson(PromptCatalog.COLOR_PLANNING_PROMPT, prompt);
            ColorAdjustmentModelOutput output = modelClient.readJson(json, ColorAdjustmentModelOutput.class);
            return new ColorAdjustment(
                    null,
                    photoId,
                    blankToDefault(output.style(), blankToDefault(targetStyle, "自然通透")),
                    blankToDefault(output.reason(), "模型未返回原因"),
                    output.basic() == null ? fallbackAdjustment.basic() : output.basic(),
                    output.hsl() == null ? fallbackAdjustment.hsl() : output.hsl(),
                    output.effects() == null ? new LightroomEffectsParams(0, 0) : output.effects(),
                    output.extended() == null ? Map.of() : output.extended(),
                    output.steps() == null ? List.of("模型未返回步骤") : output.steps(),
                    Map.of("provider", properties.activeProvider(), "rawJson", json),
                    Instant.now()
            );
        } catch (Exception exception) {
            if (properties.isFallbackEnabled()) {
                ColorAdjustment adjustment = fallback.plan(photoId, targetStyle, analysis, knowledgeItems);
                return new ColorAdjustment(
                        adjustment.id(),
                        adjustment.photoId(),
                        adjustment.style(),
                        adjustment.reason(),
                        adjustment.basic(),
                        adjustment.hsl(),
                        adjustment.effects(),
                        adjustment.extended(),
                        adjustment.steps(),
                        Map.of("fallbackReason", exception.getMessage(), "fallback", adjustment.rawResponse()),
                        adjustment.createdAt()
                );
            }
            throw exception;
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
