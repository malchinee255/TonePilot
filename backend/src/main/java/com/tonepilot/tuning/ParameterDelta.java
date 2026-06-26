package com.tonepilot.tuning;

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
