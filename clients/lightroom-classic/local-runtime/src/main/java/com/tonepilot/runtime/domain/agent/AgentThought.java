package com.tonepilot.runtime.domain.agent;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;


import java.util.List;

/**
 * 主 Agent 面向用户展示的判断结果。
 * 这里展示的是可解释的阶段性判断，不暴露模型隐藏推理链。
 */
public record AgentThought(
        String summary,
        List<String> observations,
        String reasoningVisible,
        String decision,
        String nextAction,
        List<String> toolPlan,
        List<String> userOptions
) {
    public static AgentThought empty() {
        return new AgentThought("", List.of(), "", "", "", List.of(), List.of());
    }
}
