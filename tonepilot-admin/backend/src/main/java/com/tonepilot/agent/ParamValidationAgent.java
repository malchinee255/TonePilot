package com.tonepilot.agent;

import com.tonepilot.colorgrading.domain.ColorAdjustment;

public interface ParamValidationAgent {

    ParamValidationResult validate(ColorAdjustment adjustment);
}
