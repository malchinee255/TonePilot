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
import com.tonepilot.infrastructure.ai.dto.StyleKnowledgeModelOutput;
import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.domain.style.StyleAnalysisResult;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.domain.style.StyleSample;
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

    @Autowired
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


