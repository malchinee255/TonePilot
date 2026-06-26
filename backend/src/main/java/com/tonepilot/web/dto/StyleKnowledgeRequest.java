package com.tonepilot.web.dto;

import java.util.List;
import java.util.Map;

public record StyleKnowledgeRequest(
        String title,
        String scene,
        String targetStyle,
        List<String> problems,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content,
        String status
) {
}
