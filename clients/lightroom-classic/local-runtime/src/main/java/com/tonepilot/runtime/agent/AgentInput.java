package com.tonepilot.runtime.agent;

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
