package com.tonepilot.domain.agent;

import java.util.Map;
import java.util.List;

public record AgentInput(
        String message,
        Map<String, Object> currentSettings,
        List<Map<String, Object>> knowledgeMatches,
        Map<String, Object> photoMetadata,
        String previewUrl
) {
    public AgentInput(String message, Map<String, Object> currentSettings) {
        this(message, currentSettings, List.of(), Map.of(), "");
    }

    public AgentInput(String message, Map<String, Object> currentSettings, List<Map<String, Object>> knowledgeMatches) {
        this(message, currentSettings, knowledgeMatches, Map.of(), "");
    }
}
