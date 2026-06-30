package com.tonepilot.infrastructure.agent;

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
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;







import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.domain.style.StyleAnalysisResult;
import com.tonepilot.domain.style.StyleSample;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RuleBasedStyleAnalysisAgent implements StyleAnalysisAgent {

    @Override
    public StyleAnalysisResult analyze(ColorStyle style, StyleSample sample) {
        boolean clearPortrait = style.styleName().contains("清透") || style.styleName().contains("日系");
        boolean night = style.styleName().contains("夜景") || style.styleName().contains("电影");

        if (night) {
            return new StyleAnalysisResult(
                    "夜景人像",
                    "人物",
                    "低饱和电影感",
                    "略偏冷或中性",
                    "中高对比",
                    "压低高光以保留灯光细节",
                    "适度提升阴影，避免主体死黑",
                    "提高橙色明度，控制黄色和绿色干扰",
                    Map.of(
                            "orange", "orangeLuminance +5 ~ +18",
                            "yellow", "yellowSaturation -10 ~ -25",
                            "green", "greenSaturation -15 ~ -35",
                            "blue", "blueSaturation -5 ~ -18"
                    ),
                    List.of("夜景人像", "城市街拍", "霓虹环境"),
                    List.of("严重过曝", "高调清新人像"),
                    Map.of(
                            "highlights", "-30 ~ -60",
                            "shadows", "+15 ~ +40",
                            "greenSaturation", "-15 ~ -35",
                            "grain", "0 ~ +15"
                    ),
                    "该风格适合夜景和城市氛围，核心是压高光、提主体暗部、降低杂色饱和度。"
            );
        }

        if (clearPortrait) {
            return new StyleAnalysisResult(
                    "白天人像",
                    "人物",
                    "明亮低对比",
                    "略偏暖",
                    "偏低",
                    "适度压低高光，保留皮肤和背景层次",
                    "轻微提升阴影，让画面更通透",
                    "提高橙色明度，轻微降低橙色饱和度",
                    Map.of(
                            "orange", "orangeLuminance +5 ~ +20",
                            "green", "greenSaturation -15 ~ -35",
                            "blue", "blueLuminance +5 ~ +20"
                    ),
                    List.of("白天人像", "校园写真", "自然光人像", "绿色环境"),
                    List.of("夜景", "强烈舞台光", "严重过曝照片"),
                    Map.of(
                            "contrast", "-20 ~ +5",
                            "highlights", "-30 ~ 0",
                            "shadows", "+10 ~ +40",
                            "orangeLuminance", "+5 ~ +20",
                            "greenSaturation", "-15 ~ -35"
                    ),
                    "该风格适合自然光人像，核心是降低对比、提亮阴影、弱化绿色并优化肤色。"
            );
        }

        return new StyleAnalysisResult(
                "通用场景",
                "未知主体",
                "自然低饱和",
                "中性",
                "中低",
                "保留高光层次",
                "轻微恢复暗部",
                "保持肤色自然",
                Map.of("orange", "orangeLuminance +0 ~ +10", "green", "greenSaturation -5 ~ -20"),
                style.suitableScenes(),
                style.avoidScenes(),
                Map.of("contrast", "-10 ~ +10", "shadows", "+0 ~ +20", "saturation", "-10 ~ +5"),
                "当前使用本地规则分析，适合作为后续多模态模型输出结构的占位。"
        );
    }
}
