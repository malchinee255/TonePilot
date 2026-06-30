package com.tonepilot.infrastructure.agent;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;







import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.infrastructure.ai.AiProperties;
import com.tonepilot.infrastructure.ai.OpenAiCompatibleModelClient;
import com.tonepilot.infrastructure.ai.dto.PhotoAnalysisModelOutput;
import com.tonepilot.domain.photo.Photo;
import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.domain.storage.ObjectStorageService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class ModelBackedImageAnalysisAgent implements ImageAnalysisAgent {

    private final AiProperties properties;
    private final OpenAiCompatibleModelClient modelClient;
    private final ObjectStorageService storageService;
    private final RuleBasedImageAnalysisAgent fallback;

    @Autowired
    public ModelBackedImageAnalysisAgent(
            AiProperties properties,
            OpenAiCompatibleModelClient modelClient,
            ObjectStorageService storageService,
            RuleBasedImageAnalysisAgent fallback
    ) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.storageService = storageService;
        this.fallback = fallback;
    }

    @Override
    public PhotoAnalysis analyze(Photo photo) {
        if (!properties.modelEnabled()) {
            return fallback.analyze(photo);
        }
        try {
            String imageDataUrl = storageService.readAsDataUrl(photo.fileUrl());
            String prompt = """
                    请分析这张照片，并只输出 JSON。
                    必须包含字段：scene, subject, exposureIssues, whiteBalanceIssues, colorIssues, recommendedStyles, summary。
                    数组字段即使没有明显问题，也请返回空数组。
                    """;
            String json = modelClient.completeVisionJson(PromptCatalog.PHOTO_ANALYSIS_PROMPT, prompt, imageDataUrl);
            PhotoAnalysisModelOutput output = modelClient.readJson(json, PhotoAnalysisModelOutput.class);
            return new PhotoAnalysis(
                    null,
                    photo.id(),
                    blankToDefault(output.scene(), "通用场景"),
                    blankToDefault(output.subject(), "未知主体"),
                    safeList(output.exposureIssues()),
                    safeList(output.whiteBalanceIssues()),
                    safeList(output.colorIssues()),
                    safeList(output.recommendedStyles()),
                    blankToDefault(output.summary(), "模型未返回总结"),
                    Map.of("provider", properties.activeProvider(), "rawJson", json),
                    Instant.now()
            );
        } catch (Exception exception) {
            if (properties.isFallbackEnabled()) {
                PhotoAnalysis analysis = fallback.analyze(photo);
                return new PhotoAnalysis(
                        analysis.id(),
                        analysis.photoId(),
                        analysis.scene(),
                        analysis.subject(),
                        analysis.exposureIssues(),
                        analysis.whiteBalanceIssues(),
                        analysis.colorIssues(),
                        analysis.recommendedStyles(),
                        analysis.summary(),
                        Map.of("fallbackReason", exception.getMessage(), "fallback", analysis.rawResponse()),
                        analysis.createdAt()
                );
            }
            throw exception;
        }
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}


