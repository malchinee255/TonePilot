package com.tonepilot.runtime.domain.agent;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;


import java.util.List;
import java.util.Map;

public record AgentTuneResult(
        String assistantMessage,
        AgentThought agentThought,
        Map<String, Object> developSettings,
        List<AgentDelta> deltas,
        Map<String, Object> analysis,
        List<Map<String, Object>> localAdjustments,
        String rawModelContent
) {
    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, List.of(), "");
    }

    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis,
            String rawModelContent
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, List.of(), rawModelContent);
    }

    public AgentTuneResult(
            String assistantMessage,
            Map<String, Object> developSettings,
            List<AgentDelta> deltas,
            Map<String, Object> analysis,
            List<Map<String, Object>> localAdjustments,
            String rawModelContent
    ) {
        this(assistantMessage, AgentThought.empty(), developSettings, deltas, analysis, localAdjustments, rawModelContent);
    }
}
