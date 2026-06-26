package com.tonepilot.workflow;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public String getRunId() {
        return runId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public Long getPhotoId() {
        return photoId;
    }

    public String getTargetStyle() {
        return targetStyle;
    }

    public String getProvider() {
        return provider;
    }

    public String getCurrentAgent() {
        return currentAgent;
    }

    public void setCurrentAgent(String currentAgent) {
        this.currentAgent = currentAgent;
        touch();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        touch();
    }

    public PhotoAnalysis getPhotoAnalysis() {
        return photoAnalysis;
    }

    public void setPhotoAnalysis(PhotoAnalysis photoAnalysis) {
        this.photoAnalysis = photoAnalysis;
        touch();
    }

    public String getRetrievalQuery() {
        return retrievalQuery;
    }

    public void setRetrievalQuery(String retrievalQuery) {
        this.retrievalQuery = retrievalQuery;
        touch();
    }

    public List<RagSearchItem> getRetrievedKnowledge() {
        return retrievedKnowledge;
    }

    public void setRetrievedKnowledge(List<RagSearchItem> retrievedKnowledge) {
        this.retrievedKnowledge = retrievedKnowledge == null ? List.of() : retrievedKnowledge;
        touch();
    }

    public ColorAdjustment getDraftAdjustment() {
        return draftAdjustment;
    }

    public void setDraftAdjustment(ColorAdjustment draftAdjustment) {
        this.draftAdjustment = draftAdjustment;
        touch();
    }

    public ColorAdjustment getValidatedAdjustment() {
        return validatedAdjustment;
    }

    public void setValidatedAdjustment(ColorAdjustment validatedAdjustment) {
        this.validatedAdjustment = validatedAdjustment;
        touch();
    }

    public List<String> getValidationMessages() {
        return validationMessages;
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
