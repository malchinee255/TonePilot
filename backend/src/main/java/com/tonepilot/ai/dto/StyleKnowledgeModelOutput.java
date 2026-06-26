package com.tonepilot.ai.dto;

import java.util.List;
import java.util.Map;

public record StyleKnowledgeModelOutput(
        String title,
        String scene,
        List<String> problems,
        String targetStyle,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content
) {
}
