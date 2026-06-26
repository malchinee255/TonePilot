package com.tonepilot.agent;

import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedImageAnalysisAgent implements ImageAnalysisAgent {

    @Override
    public PhotoAnalysis analyze(Photo photo) {
        String name = photo.fileName().toLowerCase();
        if (name.contains("night") || name.contains("夜") || name.contains("neon")) {
            return new PhotoAnalysis(
                    null,
                    photo.id(),
                    "夜景人像",
                    "人物",
                    List.of("人物面部偏暗", "灯光高光偏亮"),
                    List.of("整体偏暖"),
                    List.of("背景绿色偏脏", "肤色略黄"),
                    List.of("夜景电影感", "低饱和人像"),
                    "照片疑似夜景人像，适合压低高光、提升阴影，并减少背景杂色。",
                    Map.of("adapter", "rule-based", "prompt", PromptCatalog.PHOTO_ANALYSIS_PROMPT),
                    Instant.now()
            );
        }

        if (name.contains("portrait") || name.contains("人像") || name.contains("campus")) {
            return new PhotoAnalysis(
                    null,
                    photo.id(),
                    "白天人像",
                    "人物",
                    List.of("画面略偏灰", "人物肤色略暗"),
                    List.of("色温略偏冷"),
                    List.of("绿色偏脏", "肤色不够干净"),
                    List.of("日系清透", "低对比自然人像"),
                    "照片疑似自然光人像，适合降低对比、提升阴影、弱化绿色并优化肤色。",
                    Map.of("adapter", "rule-based", "prompt", PromptCatalog.PHOTO_ANALYSIS_PROMPT),
                    Instant.now()
            );
        }

        return new PhotoAnalysis(
                null,
                photo.id(),
                "通用场景",
                "未知主体",
                List.of("需要确认曝光层次"),
                List.of("需要确认白平衡"),
                List.of("整体色彩倾向不明确"),
                List.of("低饱和纪实", "自然通透"),
                "当前使用本地规则分析，建议接入多模态模型后获得更准确的照片理解。",
                Map.of("adapter", "rule-based", "prompt", PromptCatalog.PHOTO_ANALYSIS_PROMPT),
                Instant.now()
        );
    }
}
