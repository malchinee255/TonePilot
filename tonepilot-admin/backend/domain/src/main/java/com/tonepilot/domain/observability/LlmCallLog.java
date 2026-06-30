package com.tonepilot.domain.observability;

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







import java.time.Instant;

public record LlmCallLog(
        String id,
        String runId,
        String provider,
        String modelName,
        String taskType,
        boolean success,
        long latencyMs,
        int promptChars,
        int responseChars,
        String promptPreview,
        String responsePreview,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
}
