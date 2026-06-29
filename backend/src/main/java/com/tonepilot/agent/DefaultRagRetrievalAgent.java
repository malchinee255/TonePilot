package com.tonepilot.agent;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.service.RagService;
import com.tonepilot.web.dto.RagSearchItem;
import com.tonepilot.workflow.TonePilotAgentContext;
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


