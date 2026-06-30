package com.tonepilot.repository.observability;

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






import com.tonepilot.domain.observability.AuditEvent;
import com.tonepilot.domain.observability.LlmCallLog;

import java.util.List;

public interface ObservabilityLogRepository {

    void saveLlmCall(LlmCallLog item);

    void saveAuditEvent(AuditEvent item);

    List<LlmCallLog> latestLlmCalls(int limit);

    List<AuditEvent> latestAuditEvents(int limit);
}
