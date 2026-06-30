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







import com.tonepilot.domain.photo.Photo;
import com.tonepilot.domain.photo.PhotoAnalysis;
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
