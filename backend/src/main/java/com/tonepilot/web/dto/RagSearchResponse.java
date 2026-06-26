package com.tonepilot.web.dto;

import java.util.List;

public record RagSearchResponse(
        String query,
        List<RagSearchItem> items
) {
}
