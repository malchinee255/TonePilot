package com.tonepilot.agent;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.ai.AiProperties;
import com.tonepilot.ai.OpenAiCompatibleModelClient;
import com.tonepilot.ai.dto.PhotoAnalysisModelOutput;
import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.service.ObjectStorageService;
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


