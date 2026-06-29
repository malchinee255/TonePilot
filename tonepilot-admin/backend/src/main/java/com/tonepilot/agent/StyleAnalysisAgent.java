package com.tonepilot.agent;

import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleSample;

public interface StyleAnalysisAgent {

    StyleAnalysisResult analyze(ColorStyle style, StyleSample sample);
}
