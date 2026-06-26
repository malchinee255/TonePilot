package com.tonepilot.agent;

import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.domain.StyleSample;

public interface KnowledgeGenerationAgent {

    StyleKnowledge generate(ColorStyle style, StyleSample sample, StyleAnalysisResult analysis);
}
