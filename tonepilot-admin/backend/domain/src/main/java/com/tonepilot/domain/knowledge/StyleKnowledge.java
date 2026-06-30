package com.tonepilot.domain.knowledge;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StyleKnowledge(
        Long id,
        Long styleId,
        Long sampleId,
        String title,
        String scene,
        String targetStyle,
        List<String> problems,
        List<String> strategy,
        Map<String, String> paramRanges,
        String content,
        String embeddingId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
