package com.tonepilot.application.agent.workflow;

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


public interface AgentNode {

    int order();

    String stepName();

    String agentName();

    default int maxRetries() {
        return 1;
    }

    boolean shouldExecute(TonePilotAgentContext context);

    void execute(TonePilotAgentContext context);

    default String inputSummary(TonePilotAgentContext context) {
        return "photoId=%s, targetStyle=%s, provider=%s".formatted(
                context.getPhotoId(),
                blankToDefault(context.getTargetStyle(), "未指定"),
                blankToDefault(context.getProvider(), "默认配置")
        );
    }

    default String outputSummary(TonePilotAgentContext context) {
        if (context.finalAdjustment() != null) {
            return "style=%s, validationMessages=%d".formatted(
                    context.finalAdjustment().style(),
                    context.getValidationMessages().size()
            );
        }
        if (context.getRetrievedKnowledge() != null && !context.getRetrievedKnowledge().isEmpty()) {
            return "retrievedKnowledge=%d".formatted(context.getRetrievedKnowledge().size());
        }
        if (context.getPhotoAnalysis() != null) {
            return "scene=%s, subject=%s".formatted(context.getPhotoAnalysis().scene(), context.getPhotoAnalysis().subject());
        }
        return "尚无输出";
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
