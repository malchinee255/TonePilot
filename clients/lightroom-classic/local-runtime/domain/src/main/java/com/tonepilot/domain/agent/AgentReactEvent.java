package com.tonepilot.domain.agent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ReAct 执行过程中的可观测事件。
 * 这里记录的是可以展示给用户和管理端审计的观察、判断和动作，不暴露模型隐藏推理链。
 */
public record AgentReactEvent(
        String type,
        String title,
        String content,
        Map<String, Object> payload,
        Instant createdAt
) {
    public static AgentReactEvent of(String type, String title, String content, Map<String, Object> payload) {
        return new AgentReactEvent(
                type,
                title,
                content,
                payload == null ? Map.of() : payload,
                Instant.now()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", type);
        value.put("title", title);
        value.put("content", content);
        value.put("payload", payload == null ? Map.of() : payload);
        value.put("createdAt", createdAt == null ? "" : createdAt.toString());
        return value;
    }
}
