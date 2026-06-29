package com.tonepilot.workflow;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class TonePilotAgentContext {

    private final String runId = UUID.randomUUID().toString();
    private final Instant createdAt = Instant.now();
    private Instant updatedAt = createdAt;
    private String status = "created";
    private final Long photoId;
    private final String targetStyle;
    private final String provider;
    private String currentAgent;
    private String errorMessage;
    private PhotoAnalysis photoAnalysis;
    private String retrievalQuery;
    private List<RagSearchItem> retrievedKnowledge = List.of();
    private ColorAdjustment draftAdjustment;
    private ColorAdjustment validatedAdjustment;
    private List<String> validationMessages = List.of();
    private final List<AgentStepTrace> stepTraces = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public TonePilotAgentContext(Long photoId, String targetStyle, String provider) {
        this.photoId = photoId;
        this.targetStyle = targetStyle;
        this.provider = provider;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public void setCurrentAgent(String currentAgent) {
        this.currentAgent = currentAgent;
        touch();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        touch();
    }

    public void setPhotoAnalysis(PhotoAnalysis photoAnalysis) {
        this.photoAnalysis = photoAnalysis;
        touch();
    }

    public void setRetrievalQuery(String retrievalQuery) {
        this.retrievalQuery = retrievalQuery;
        touch();
    }

    public void setRetrievedKnowledge(List<RagSearchItem> retrievedKnowledge) {
        this.retrievedKnowledge = retrievedKnowledge == null ? List.of() : retrievedKnowledge;
        touch();
    }

    public void setDraftAdjustment(ColorAdjustment draftAdjustment) {
        this.draftAdjustment = draftAdjustment;
        touch();
    }

    public void setValidatedAdjustment(ColorAdjustment validatedAdjustment) {
        this.validatedAdjustment = validatedAdjustment;
        touch();
    }

    public void setValidationMessages(List<String> validationMessages) {
        this.validationMessages = validationMessages == null ? List.of() : validationMessages;
        touch();
    }

    public List<AgentStepTrace> getStepTraces() {
        return List.copyOf(stepTraces);
    }

    public void addStepTrace(AgentStepTrace stepTrace) {
        this.stepTraces.add(stepTrace);
        touch();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public ColorAdjustment finalAdjustment() {
        return validatedAdjustment == null ? draftAdjustment : validatedAdjustment;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
