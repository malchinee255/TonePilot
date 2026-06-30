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


import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.infrastructure.ai.AiProperties;
import com.tonepilot.infrastructure.ai.OpenAiCompatibleModelClient;
import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.domain.style.StyleAnalysisResult;
import com.tonepilot.domain.style.StyleSample;
import com.tonepilot.domain.storage.ObjectStorageService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ModelBackedStyleAnalysisAgent implements StyleAnalysisAgent {

    private final AiProperties properties;
    private final OpenAiCompatibleModelClient modelClient;
    private final ObjectStorageService storageService;
    private final RuleBasedStyleAnalysisAgent fallback;

    @Autowired
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


