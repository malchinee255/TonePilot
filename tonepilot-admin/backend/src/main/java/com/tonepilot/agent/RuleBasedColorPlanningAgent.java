package com.tonepilot.agent;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.web.dto.RagSearchItem;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedColorPlanningAgent implements ColorPlanningAgent {

    @Override
    public ColorAdjustment plan(Long photoId, String targetStyle, PhotoAnalysis analysis, List<RagSearchItem> knowledgeItems) {
        String style = isBlank(targetStyle)
                ? analysis.recommendedStyles().stream().findFirst().orElse("自然通透")
                : targetStyle;

        boolean highlightProblem = containsAny(analysis.exposureIssues(), "高光", "过曝", "偏亮", "溢出");
        boolean shadowProblem = containsAny(analysis.exposureIssues(), "偏暗", "欠曝", "暗部");
        boolean greenDirty = containsAny(analysis.colorIssues(), "绿色", "偏脏");
        boolean skinYellow = containsAny(analysis.colorIssues(), "肤色", "偏黄");
        boolean filmStyle = style.contains("胶片") || style.toLowerCase().contains("film");
        boolean clearStyle = style.contains("清透") || style.contains("通透");
        boolean cinematicStyle = style.contains("电影");

        LightroomBasicParams basic = new LightroomBasicParams(
                highlightProblem ? -0.05 : 0.15,
                clearStyle ? -12 : cinematicStyle ? 12 : 4,
                highlightProblem ? -45 : -18,
                shadowProblem ? 30 : 14,
                highlightProblem ? -12 : 4,
                cinematicStyle ? -18 : -6,
                analysis.whiteBalanceIssues().stream().anyMatch(item -> item.contains("偏暖")) ? -4 : 2,
                analysis.whiteBalanceIssues().stream().anyMatch(item -> item.contains("偏绿")) ? 6 : 2,
                clearStyle ? 2 : 5,
                cinematicStyle ? 8 : 3,
                cinematicStyle ? 6 : 1,
                clearStyle ? 12 : 6,
                clearStyle ? -4 : -2
        );

        LightroomHslParams hsl = new LightroomHslParams(
                0, -3, 0,
                skinYellow ? -2 : 0, skinYellow ? -2 : 4, skinYellow ? 12 : 6,
                -5, skinYellow ? -18 : -8, 0,
                greenDirty ? -10 : -4, greenDirty ? -30 : -12, greenDirty ? -5 : 0,
                0, -8, 0,
                clearStyle ? -2 : -5, clearStyle ? -8 : -12, clearStyle ? 8 : -4,
                0, 0, 0,
                0, 0, 0
        );

        LightroomEffectsParams effects = new LightroomEffectsParams(
                filmStyle ? 18 : cinematicStyle ? 8 : 0,
                cinematicStyle ? -10 : filmStyle ? -6 : 0
        );

        List<String> steps = new ArrayList<>();
        if (highlightProblem) {
            steps.add("降低高光，优先保留灯光和亮部细节。");
        }
        if (shadowProblem) {
            steps.add("提升阴影，让主体暗部恢复可读性。");
        }
        if (greenDirty) {
            steps.add("降低绿色饱和度并微调绿色色相，减少背景杂色。");
        }
        if (skinYellow) {
            steps.add("提高橙色明度并轻微控制橙色饱和度，让肤色更干净。");
        }
        if (steps.isEmpty()) {
            steps.add("采用保守调整，优先保持自然观感和后续可编辑空间。");
        }

        String reason = "基于照片场景「%s」、问题点「%s」以及检索到的 %d 条知识，生成偏保守的 %s Lightroom 参数。"
                .formatted(analysis.scene(), String.join("、", merge(analysis.exposureIssues(), analysis.colorIssues())), knowledgeItems.size(), style);

        return new ColorAdjustment(
                null,
                photoId,
                style,
                reason,
                basic,
                hsl,
                effects,
                steps,
                Map.of(
                        "adapter", "rule-based",
                        "prompt", PromptCatalog.COLOR_PLANNING_PROMPT,
                        "knowledgeCount", knowledgeItems.size()
                ),
                Instant.now()
        );
    }

    private boolean containsAny(List<String> values, String... keywords) {
        return values.stream().anyMatch(value -> {
            for (String keyword : keywords) {
                if (value.contains(keyword)) {
                    return true;
                }
            }
            return false;
        });
    }

    private List<String> merge(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>(first);
        values.addAll(second);
        return values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
