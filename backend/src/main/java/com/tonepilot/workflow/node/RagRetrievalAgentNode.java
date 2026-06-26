package com.tonepilot.workflow.node;

import com.tonepilot.agent.RagRetrievalAgent;
import com.tonepilot.workflow.AgentNode;
import com.tonepilot.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

@Component
public class RagRetrievalAgentNode implements AgentNode {

    private final RagRetrievalAgent ragRetrievalAgent;

    public RagRetrievalAgentNode(RagRetrievalAgent ragRetrievalAgent) {
        this.ragRetrievalAgent = ragRetrievalAgent;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public String stepName() {
        return "style-retrieval";
    }

    @Override
    public String agentName() {
        return "RagRetrievalAgent";
    }

    @Override
    public boolean shouldExecute(TonePilotAgentContext context) {
        return context.getPhotoAnalysis() != null;
    }

    @Override
    public void execute(TonePilotAgentContext context) {
        ragRetrievalAgent.retrieve(context);
    }
}
