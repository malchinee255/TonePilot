package com.tonepilot.agent;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;

import java.util.List;

public interface ColorPlanningAgent {

    ColorAdjustment plan(Long photoId, String targetStyle, PhotoAnalysis analysis, List<RagSearchItem> knowledgeItems);
}
