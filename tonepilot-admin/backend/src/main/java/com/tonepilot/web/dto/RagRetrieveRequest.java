package com.tonepilot.web.dto;

public record RagRetrieveRequest(
        String query,
        int topK
) {
}
