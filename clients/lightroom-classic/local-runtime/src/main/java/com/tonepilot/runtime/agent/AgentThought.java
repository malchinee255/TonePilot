package com.tonepilot.runtime.agent;

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
