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

import com.tonepilot.domain.common.NotFoundException;
import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.infrastructure.observability.ObservabilityService;
import com.tonepilot.infrastructure.shared.persistence.DomainSnapshotRepository;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import com.tonepilot.application.dto.AdjustmentGenerateRequest;
import com.tonepilot.domain.agent.workflow.TonePilotAgentContext;
import com.tonepilot.application.agent.workflow.WorkflowRunRepository;
import com.tonepilot.domain.agent.workflow.WorkflowRunSnapshot;
import com.tonepilot.application.agent.workflow.TonePilotWorkflowOrchestrator;
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


