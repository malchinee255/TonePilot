package com.tonepilot.infrastructure.agent;

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


import com.tonepilot.infrastructure.ai.AiProperties;
import com.tonepilot.infrastructure.ai.OpenAiCompatibleModelClient;
import com.tonepilot.infrastructure.ai.dto.ColorAdjustmentModelOutput;
import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.domain.colorgrading.LightroomEffectsParams;
import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.server.dto.RagSearchItem;
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
