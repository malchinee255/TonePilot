package com.tonepilot.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record KnowledgeRequest(
        @NotBlank String title,
        @NotBlank String scene,
        List<String> problems,
        @NotBlank String targetStyle,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content
) {
}
