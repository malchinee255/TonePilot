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
