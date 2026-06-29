package com.tonepilot.agent;

import com.tonepilot.workflow.TonePilotAgentContext;

public interface RagRetrievalAgent {

    void retrieve(TonePilotAgentContext context);
}
