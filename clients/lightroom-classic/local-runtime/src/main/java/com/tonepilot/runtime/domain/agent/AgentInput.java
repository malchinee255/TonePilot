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


import java.util.Map;
import java.util.List;

public record AgentInput(
        String message,
        Map<String, Object> currentSettings,
        List<Map<String, Object>> knowledgeMatches
) {
    public AgentInput(String message, Map<String, Object> currentSettings) {
        this(message, currentSettings, List.of());
    }
}
