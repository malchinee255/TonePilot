package com.tonepilot.application.agent.workflow;

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

import com.tonepilot.infrastructure.ai.AiProviderContext;
import com.tonepilot.domain.observability.TraceContext;
import com.tonepilot.application.dto.AdjustmentGenerateRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class TonePilotWorkflowOrchestrator {

    private final WorkflowRunRepository workflowRunRepository;
    private final List<AgentNode> nodes;

    @Autowired
    public TonePilotWorkflowOrchestrator(
            WorkflowRunRepository workflowRunRepository,
            List<AgentNode> nodes
    ) {
        this.workflowRunRepository = workflowRunRepository;
        this.nodes = nodes.stream()
                .sorted(Comparator.comparingInt(AgentNode::order))
                .toList();
    }

    public TonePilotAgentContext run(AdjustmentGenerateRequest request) {
        TonePilotAgentContext context = new TonePilotAgentContext(
                request.photoId(),
                request.targetStyle(),
                request.provider()
        );
        workflowRunRepository.save(context);
        return TraceContext.useRunId(
                context.getRunId(),
                () -> AiProviderContext.use(request.provider(), () -> runInternal(context))
        );
    }

    private TonePilotAgentContext runInternal(TonePilotAgentContext context) {
        context.setStatus("running");
        for (AgentNode node : nodes) {
            executeNode(context, node);
            workflowRunRepository.save(context);
        }
        context.setStatus("completed");
        workflowRunRepository.save(context);
        return context;
    }

    private void executeNode(TonePilotAgentContext context, AgentNode node) {
        if (!node.shouldExecute(context)) {
            context.addStepTrace(new AgentStepTrace(
                    node.stepName(),
                    node.agentName(),
                    AgentStepStatus.SKIPPED,
                    Instant.now(),
                    Instant.now(),
                    0,
                    0,
                    node.inputSummary(context),
                    "条件不满足，跳过节点",
                    null,
                    Map.of()
            ));
            return;
        }

        for (int attempt = 1; attempt <= node.maxRetries() + 1; attempt++) {
            Instant startedAt = Instant.now();
            context.setCurrentAgent(node.agentName());
            try {
                node.execute(context);
                Instant finishedAt = Instant.now();
                context.addStepTrace(new AgentStepTrace(
                        node.stepName(),
                        node.agentName(),
                        AgentStepStatus.COMPLETED,
                        startedAt,
                        finishedAt,
                        Duration.between(startedAt, finishedAt).toMillis(),
                        attempt,
                        node.inputSummary(context),
                        node.outputSummary(context),
                        null,
                        Map.of()
                ));
                return;
            } catch (Exception exception) {
                Instant finishedAt = Instant.now();
                context.addStepTrace(new AgentStepTrace(
                        node.stepName(),
                        node.agentName(),
                        AgentStepStatus.FAILED,
                        startedAt,
                        finishedAt,
                        Duration.between(startedAt, finishedAt).toMillis(),
                        attempt,
                        node.inputSummary(context),
                        node.outputSummary(context),
                        exception.getMessage(),
                        Map.of()
                ));
                if (attempt > node.maxRetries()) {
                    context.setStatus("failed");
                    context.setErrorMessage(exception.getMessage());
                    workflowRunRepository.save(context);
                    throw new IllegalStateException("Agent 节点执行失败：" + node.stepName(), exception);
                }
            }
        }
    }
}


