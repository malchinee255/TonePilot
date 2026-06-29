package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.observability.ObservabilityService;
import com.tonepilot.persistence.DomainSnapshotRepository;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.AdjustmentGenerateRequest;
import com.tonepilot.workflow.TonePilotAgentContext;
import com.tonepilot.workflow.WorkflowRunRepository;
import com.tonepilot.workflow.WorkflowRunSnapshot;
import com.tonepilot.workflow.TonePilotWorkflowOrchestrator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ColorAgentService {

    private final InMemoryTonePilotStore store;
    private final TonePilotWorkflowOrchestrator workflowOrchestrator;
    private final WorkflowRunRepository workflowRunRepository;
    private final ObservabilityService observabilityService;
    private final DomainSnapshotRepository snapshotRepository;

    @Autowired
    public ColorAgentService(
            InMemoryTonePilotStore store,
            TonePilotWorkflowOrchestrator workflowOrchestrator,
            WorkflowRunRepository workflowRunRepository,
            ObservabilityService observabilityService,
            DomainSnapshotRepository snapshotRepository
    ) {
        this.store = store;
        this.workflowOrchestrator = workflowOrchestrator;
        this.workflowRunRepository = workflowRunRepository;
        this.observabilityService = observabilityService;
        this.snapshotRepository = snapshotRepository;
    }

    public ColorAdjustment generate(AdjustmentGenerateRequest request) {
        TonePilotAgentContext context = workflowOrchestrator.run(request);
        ColorAdjustment draft = context.finalAdjustment();
        ColorAdjustment saved = new ColorAdjustment(
                store.adjustmentIds.getAndIncrement(),
                request.photoId(),
                draft.style(),
                draft.reason(),
                draft.basic(),
                draft.hsl(),
                draft.effects(),
                draft.extended(),
                draft.steps(),
                workflowMetadata(context, draft),
                Instant.now()
        );
        store.adjustments.put(saved.id(), saved);
        snapshotRepository.save("color_adjustment", saved.id(), saved);
        context.setValidatedAdjustment(saved);
        workflowRunRepository.save(context);
        observabilityService.recordAuditEvent(
                "adjustment.generated",
                "system",
                context.getRunId(),
                "photo",
                String.valueOf(request.photoId()),
                "生成调色方案，style=" + saved.style()
        );
        return saved;
    }

    public WorkflowRunSnapshot getWorkflow(String runId) {
        return workflowRunRepository.find(runId)
                .orElseThrow(() -> new NotFoundException("未找到调色工作流：" + runId));
    }

    private Map<String, Object> workflowMetadata(TonePilotAgentContext context, ColorAdjustment draft) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("workflowRunId", context.getRunId());
        metadata.put("workflowStatus", context.getStatus());
        metadata.put("provider", context.getProvider() == null ? "default" : context.getProvider());
        metadata.put("retrievalQuery", context.getRetrievalQuery());
        metadata.put("ragItems", context.getRetrievedKnowledge());
        metadata.put("validationMessages", context.getValidationMessages());
        metadata.put("workflowTrace", context.getStepTraces());
        metadata.put("workflowMetadata", context.getMetadata());
        metadata.put("source", draft.rawResponse());
        return metadata;
    }
}


