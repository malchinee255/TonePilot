package com.tonepilot.agent;

import com.tonepilot.ai.AiProperties;
import com.tonepilot.ai.OpenAiCompatibleModelClient;
import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleSample;
import com.tonepilot.service.ObjectStorageService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ModelBackedStyleAnalysisAgent implements StyleAnalysisAgent {

    private final AiProperties properties;
    private final OpenAiCompatibleModelClient modelClient;
    private final ObjectStorageService storageService;
    private final RuleBasedStyleAnalysisAgent fallback;

    public ModelBackedStyleAnalysisAgent(
            AiProperties properties,
            OpenAiCompatibleModelClient modelClient,
            ObjectStorageService storageService,
            RuleBasedStyleAnalysisAgent fallback
    ) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.storageService = storageService;
        this.fallback = fallback;
    }

    @Override
    public StyleAnalysisResult analyze(ColorStyle style, StyleSample sample) {
        if (!properties.modelEnabled()) {
            return fallback.analyze(style, sample);
        }
        try {
            String prompt = """
                    请分析样本作品的调色风格，并只输出 JSON。
                    风格名称：%s
                    风格描述：%s
                    样本类型：%s
                    必须包含字段：scene, subject, toneStyle, temperatureTrend, contrastTrend,
                    highlightStrategy, shadowStrategy, skinToneStrategy, hslStrategy,
                    suitableScenes, avoidScenes, possibleParamRanges, summary。
                    注意：只能推断通用调色倾向，不要声称知道真实 Lightroom 参数。
                    """.formatted(style.styleName(), style.description(), sample.sampleType());
            String imageDataUrl = sample.finalImageUrl() != null
                    ? storageService.readAsDataUrl(sample.finalImageUrl())
                    : sample.afterImageUrl() != null ? storageService.readAsDataUrl(sample.afterImageUrl()) : null;
            String json = imageDataUrl == null
                    ? modelClient.completeJson(PromptCatalog.STYLE_ANALYSIS_PROMPT, prompt)
                    : modelClient.completeVisionJson(PromptCatalog.STYLE_ANALYSIS_PROMPT, prompt, imageDataUrl);
            return modelClient.readJson(json, StyleAnalysisResult.class);
        } catch (Exception exception) {
            if (properties.isFallbackEnabled()) {
                return fallback.analyze(style, sample);
            }
            throw exception;
        }
    }
}
