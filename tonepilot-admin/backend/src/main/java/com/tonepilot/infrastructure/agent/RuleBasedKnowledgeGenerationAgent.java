package com.tonepilot.infrastructure.agent;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.domain.style.StyleAnalysisResult;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.domain.style.StyleSample;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class RuleBasedKnowledgeGenerationAgent implements KnowledgeGenerationAgent {

    @Override
    public StyleKnowledge generate(ColorStyle style, StyleSample sample, StyleAnalysisResult analysis) {
        List<String> problems = analysis.scene().contains("夜")
                ? List.of("灯光高光偏亮", "人物偏暗", "背景杂色干扰")
                : List.of("画面偏灰", "绿色偏脏", "肤色偏暗");

        List<String> strategy = analysis.scene().contains("夜")
                ? List.of("降低高光以保留灯光细节", "提升阴影恢复人物暗部", "降低绿色和黄色饱和度", "适度添加暗角增强聚焦")
                : List.of("降低对比度让画面更柔和", "提升阴影增强通透感", "降低绿色饱和度减少背景杂色", "提高橙色明度优化肤色");

        String content = "%s 适合 %s。核心思路：%s。参数倾向：%s。"
                .formatted(style.styleName(), String.join("、", analysis.suitableScenes()), String.join("；", strategy), analysis.possibleParamRanges());

        return new StyleKnowledge(
                null,
                style.id(),
                sample.id(),
                style.styleName() + "调色策略",
                analysis.scene(),
                style.styleName(),
                problems,
                strategy,
                analysis.possibleParamRanges(),
                content,
                "local-style-" + UUID.randomUUID(),
                "pending",
                Instant.now(),
                Instant.now()
        );
    }
}
