package com.tonepilot.agent;

import com.tonepilot.ai.AiProperties;
import com.tonepilot.ai.OpenAiCompatibleModelClient;
import com.tonepilot.ai.dto.StyleKnowledgeModelOutput;
import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.domain.StyleSample;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Primary
@Component
public class ModelBackedKnowledgeGenerationAgent implements KnowledgeGenerationAgent {

    private final AiProperties properties;
    private final OpenAiCompatibleModelClient modelClient;
    private final RuleBasedKnowledgeGenerationAgent fallback;

    public ModelBackedKnowledgeGenerationAgent(
            AiProperties properties,
            OpenAiCompatibleModelClient modelClient,
            RuleBasedKnowledgeGenerationAgent fallback
    ) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.fallback = fallback;
    }

    @Override
    public StyleKnowledge generate(ColorStyle style, StyleSample sample, StyleAnalysisResult analysis) {
        if (!properties.modelEnabled()) {
            return fallback.generate(style, sample, analysis);
        }
        try {
            String prompt = """
                    请根据风格分析结果生成一条可用于 RAG 检索的调色知识。
                    风格：%s
                    分析结果：%s
                    只输出 JSON，字段必须为 title, scene, problems, targetStyle, strategy, paramRanges, content。
                    不要写复刻、完全还原、盗取预设等表达，只沉淀通用调色方法论。
                    """.formatted(style.styleName(), modelClient.writeJson(analysis));
            String json = modelClient.completeJson(PromptCatalog.KNOWLEDGE_GENERATION_PROMPT, prompt);
            StyleKnowledgeModelOutput output = modelClient.readJson(json, StyleKnowledgeModelOutput.class);
            return new StyleKnowledge(
                    null,
                    style.id(),
                    sample.id(),
                    blankToDefault(output.title(), style.styleName() + "调色策略"),
                    blankToDefault(output.scene(), analysis.scene()),
                    blankToDefault(output.targetStyle(), style.styleName()),
                    safeList(output.problems()),
                    safeList(output.strategy()),
                    output.paramRanges() == null ? Map.of() : output.paramRanges(),
                    blankToDefault(output.content(), analysis.summary()),
                    "model-style-" + UUID.randomUUID(),
                    "pending",
                    Instant.now(),
                    Instant.now()
            );
        } catch (Exception exception) {
            if (properties.isFallbackEnabled()) {
                return fallback.generate(style, sample, analysis);
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
