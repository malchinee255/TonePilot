package com.tonepilot.agent;

import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.domain.StyleSample;
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
