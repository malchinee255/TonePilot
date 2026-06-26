package com.tonepilot.agent;

import com.tonepilot.domain.ColorAdjustment;

public interface ParamValidationAgent {

    ParamValidationResult validate(ColorAdjustment adjustment);
}
