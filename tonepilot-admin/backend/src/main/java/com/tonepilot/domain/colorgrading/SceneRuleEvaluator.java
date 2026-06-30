package com.tonepilot.domain.colorgrading;

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


import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.domain.photo.PhotoAnalysis;

import java.util.ArrayList;
import java.util.List;

public class SceneRuleEvaluator {

    public List<String> evaluate(PhotoAnalysis analysis, ColorAdjustment adjustment) {
        List<String> issues = new ArrayList<>();

        if (contains(analysis.exposureIssues(), "高光", "过曝", "偏亮")) {
            issues.add(adjustment.basic().highlights() < 0
                    ? "highlights 与高光问题匹配"
                    : "照片有高光问题，但 highlights 未降低");
        }

        if (contains(analysis.exposureIssues(), "偏暗", "欠曝", "暗部")) {
            issues.add(adjustment.basic().shadows() > 0
                    ? "shadows 与暗部问题匹配"
                    : "照片有暗部问题，但 shadows 未提升");
        }

        if (contains(analysis.colorIssues(), "绿色", "偏脏")) {
            issues.add(adjustment.hsl().greenSaturation() < 0
                    ? "greenSaturation 与绿色偏脏问题匹配"
                    : "照片有绿色偏脏问题，但 greenSaturation 未降低");
        }

        if (adjustment.style().contains("胶片")) {
            issues.add(adjustment.effects().grain() > 0
                    ? "胶片风格包含合理颗粒"
                    : "胶片风格建议增加适度 grain");
        }

        if (issues.isEmpty()) {
            issues.add("未触发特殊场景规则，参数整体保守");
        }

        return issues;
    }

    private boolean contains(List<String> values, String... keywords) {
        return values.stream().anyMatch(value -> {
            for (String keyword : keywords) {
                if (value.contains(keyword)) {
                    return true;
                }
            }
            return false;
        });
    }
}
