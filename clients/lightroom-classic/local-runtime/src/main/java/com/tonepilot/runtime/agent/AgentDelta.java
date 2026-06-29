package com.tonepilot.runtime.agent;

public record AgentDelta(
        String group,
        String name,
        String label,
        Object beforeValue,
        Object afterValue,
        Object delta,
        String reason
) {
}
