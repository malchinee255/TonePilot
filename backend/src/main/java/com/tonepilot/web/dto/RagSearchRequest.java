package com.tonepilot.web.dto;

public record RagSearchRequest(
        Long photoId,
        String query,
        Integer topK
) {
}
