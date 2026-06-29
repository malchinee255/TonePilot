package com.tonepilot.workflow.node;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.agent.ColorPlanningAgent;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.workflow.AgentNode;
import com.tonepilot.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

@Component
public class ColorPlanningAgentNode implements AgentNode {

    private final ColorPlanningAgent colorPlanningAgent;

    @Autowired
    public ColorPlanningAgentNode(ColorPlanningAgent colorPlanningAgent) {
        this.colorPlanningAgent = colorPlanningAgent;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public String stepName() {
        return "color-planning";
    }

    @Override
    public String agentName() {
        return "ColorPlanningAgent";
    }

    @Override
    public boolean shouldExecute(TonePilotAgentContext context) {
        return context.getPhotoAnalysis() != null;
    }

    @Override
    public void execute(TonePilotAgentContext context) {
        ColorAdjustment draft = colorPlanningAgent.plan(
                context.getPhotoId(),
                context.getTargetStyle(),
                context.getPhotoAnalysis(),
                context.getRetrievedKnowledge()
        );
        context.setDraftAdjustment(draft);
    }
}


