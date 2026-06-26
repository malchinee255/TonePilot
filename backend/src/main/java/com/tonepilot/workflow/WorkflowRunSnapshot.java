package com.tonepilot.workflow;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowRunSnapshot(
        String runId,
        Instant createdAt,
        Instant updatedAt,
        String status,
        Long photoId,
        String targetStyle,
        String provider,
        String currentAgent,
        String errorMessage,
        PhotoAnalysis photoAnalysis,
        String retrievalQuery,
        List<RagSearchItem> retrievedKnowledge,
        ColorAdjustment draftAdjustment,
        ColorAdjustment validatedAdjustment,
        List<String> validationMessages,
        List<AgentStepTrace> stepTraces,
        Map<String, Object> metadata,
        String storage
) {

    public static WorkflowRunSnapshot from(TonePilotAgentContext context, String storage) {
        return new WorkflowRunSnapshot(
                context.getRunId(),
                context.getCreatedAt(),
                context.getUpdatedAt(),
                context.getStatus(),
                context.getPhotoId(),
                context.getTargetStyle(),
                context.getProvider(),
                context.getCurrentAgent(),
                context.getErrorMessage(),
                context.getPhotoAnalysis(),
                context.getRetrievalQuery(),
                context.getRetrievedKnowledge(),
                context.getDraftAdjustment(),
                context.getValidatedAdjustment(),
                context.getValidationMessages(),
                context.getStepTraces(),
                context.getMetadata(),
                storage
        );
    }

    public WorkflowRunSnapshot withStorage(String storage) {
        return new WorkflowRunSnapshot(
                runId,
                createdAt,
                updatedAt,
                status,
                photoId,
                targetStyle,
                provider,
                currentAgent,
                errorMessage,
                photoAnalysis,
                retrievalQuery,
                retrievedKnowledge,
                draftAdjustment,
                validatedAdjustment,
                validationMessages,
                stepTraces,
                metadata,
                storage
        );
    }
}
