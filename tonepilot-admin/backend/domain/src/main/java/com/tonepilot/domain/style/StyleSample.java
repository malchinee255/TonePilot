package com.tonepilot.domain.style;

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

public record StyleSample(
        Long id,
        Long styleId,
        String sampleType,
        String beforeImageUrl,
        String afterImageUrl,
        String finalImageUrl,
        String sourceType,
        String description,
        List<String> tags,
        StyleAnalysisResult analysisResult,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
