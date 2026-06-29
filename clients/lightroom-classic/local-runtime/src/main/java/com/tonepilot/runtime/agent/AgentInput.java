package com.tonepilot.runtime.agent;

import java.util.Map;

public record AgentInput(
        String message,
        Map<String, Object> currentSettings
) {
}
