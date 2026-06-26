package com.tonepilot.workflow.node;

import com.tonepilot.agent.ParamValidationAgent;
import com.tonepilot.agent.ParamValidationResult;
import com.tonepilot.workflow.AgentNode;
import com.tonepilot.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

@Component
public class ParamValidationAgentNode implements AgentNode {

    private final ParamValidationAgent paramValidationAgent;

    public ParamValidationAgentNode(ParamValidationAgent paramValidationAgent) {
        this.paramValidationAgent = paramValidationAgent;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public String stepName() {
        return "param-validation";
    }

    @Override
    public String agentName() {
        return "ParamValidationAgent";
    }

    @Override
    public int maxRetries() {
        return 0;
    }

    @Override
    public boolean shouldExecute(TonePilotAgentContext context) {
        return context.getDraftAdjustment() != null;
    }

    @Override
    public void execute(TonePilotAgentContext context) {
        ParamValidationResult result = paramValidationAgent.validate(context.getDraftAdjustment());
        context.setValidatedAdjustment(result.adjustment());
        context.setValidationMessages(result.messages());
        context.getMetadata().put("validationCorrected", result.corrected());
    }
}
