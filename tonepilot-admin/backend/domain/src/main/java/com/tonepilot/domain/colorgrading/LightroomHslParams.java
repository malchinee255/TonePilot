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







public record LightroomHslParams(
        int redHue,
        int redSaturation,
        int redLuminance,
        int orangeHue,
        int orangeSaturation,
        int orangeLuminance,
        int yellowHue,
        int yellowSaturation,
        int yellowLuminance,
        int greenHue,
        int greenSaturation,
        int greenLuminance,
        int aquaHue,
        int aquaSaturation,
        int aquaLuminance,
        int blueHue,
        int blueSaturation,
        int blueLuminance,
        int purpleHue,
        int purpleSaturation,
        int purpleLuminance,
        int magentaHue,
        int magentaSaturation,
        int magentaLuminance
) {
}
