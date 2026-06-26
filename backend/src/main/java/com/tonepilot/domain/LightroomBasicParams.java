package com.tonepilot.domain;

public record LightroomBasicParams(
        double exposure,
        int contrast,
        int highlights,
        int shadows,
        int whites,
        int blacks,
        int temperature,
        int tint,
        int texture,
        int clarity,
        int dehaze,
        int vibrance,
        int saturation
) {
}
