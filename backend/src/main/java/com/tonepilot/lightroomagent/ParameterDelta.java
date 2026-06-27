package com.tonepilot.lightroomagent;

public record ParameterDelta(
        String group,
        String name,
        String label,
        String beforeValue,
        String afterValue,
        String delta,
        String reason
) {
}
