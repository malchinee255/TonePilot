package com.tonepilot.workflow;

import com.tonepilot.ai.AiProviderContext;
import com.tonepilot.observability.TraceContext;
import com.tonepilot.web.dto.AdjustmentGenerateRequest;
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
