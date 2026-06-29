package com.tonepilot.agent;

import com.tonepilot.colorgrading.domain.ColorAdjustment;

import java.util.List;

public record ParamValidationResult(
        ColorAdjustment adjustment,
        List<String> messages,
        boolean corrected
) {
}
