package com.tonepilot.application.agent;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.controller.admin.*;
import com.tonepilot.application.dto.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
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

import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.application.knowledge.RagService;
import com.tonepilot.domain.knowledge.RagSearchItem;
import com.tonepilot.domain.agent.workflow.TonePilotAgentContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultRagRetrievalAgent implements RagRetrievalAgent {

    private final RagService ragService;
    private final int defaultTopK;

    @Autowired
    public DefaultRagRetrievalAgent(
            RagService ragService,
            @Value("${tonepilot.rag.default-top-k:5}") int defaultTopK
    ) {
        this.ragService = ragService;
        this.defaultTopK = defaultTopK;
    }

    @Override
    public void retrieve(TonePilotAgentContext context) {
        String query = buildRetrievalQuery(context);
        context.setRetrievalQuery(query);
        List<RagSearchItem> knowledgeItems = ragService.retrieve(query, defaultTopK);
        context.setRetrievedKnowledge(knowledgeItems);
    }

    private String buildRetrievalQuery(TonePilotAgentContext context) {
        PhotoAnalysis analysis = context.getPhotoAnalysis();
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, analysis.scene());
        addIfPresent(parts, analysis.subject());
        addAll(parts, analysis.exposureIssues());
        addAll(parts, analysis.whiteBalanceIssues());
        addAll(parts, analysis.colorIssues());
        addAll(parts, analysis.recommendedStyles());
        addIfPresent(parts, context.getTargetStyle());
        return String.join("，", parts);
    }

    private void addAll(List<String> parts, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> addIfPresent(parts, value));
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }
}


