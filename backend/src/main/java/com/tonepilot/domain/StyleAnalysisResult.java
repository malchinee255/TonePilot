package com.tonepilot.domain;

import java.util.List;
import java.util.Map;

public record StyleAnalysisResult(
        String scene,
        String subject,
        String toneStyle,
        String temperatureTrend,
        String contrastTrend,
        String highlightStrategy,
        String shadowStrategy,
        String skinToneStrategy,
        Map<String, String> hslStrategy,
        List<String> suitableScenes,
        List<String> avoidScenes,
        Map<String, String> possibleParamRanges,
        String summary
) {
}
