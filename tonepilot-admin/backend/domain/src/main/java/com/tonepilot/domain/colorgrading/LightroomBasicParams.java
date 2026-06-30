package com.tonepilot.domain.colorgrading;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







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
