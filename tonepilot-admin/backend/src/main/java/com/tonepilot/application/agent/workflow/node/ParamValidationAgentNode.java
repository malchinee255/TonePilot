package com.tonepilot.application.agent.workflow.node;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.agent.ParamValidationAgent;
import com.tonepilot.domain.agent.ParamValidationResult;
import com.tonepilot.application.agent.workflow.AgentNode;
import com.tonepilot.application.agent.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

@Component
public class ParamValidationAgentNode implements AgentNode {

    private final ParamValidationAgent paramValidationAgent;

    @Autowired
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


