package com.tonepilot.domain.colorgrading;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import com.tonepilot.domain.colorgrading.ColorAdjustment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ParamRangeValidator {

    public List<String> validate(ColorAdjustment adjustment) {
        List<String> issues = new ArrayList<>();
        if (adjustment.basic() == null || adjustment.hsl() == null || adjustment.effects() == null) {
            issues.add("调色参数缺少 basic、hsl 或 effects 结构");
            return issues;
        }
        check(adjustment.basic().exposure() >= -1.5 && adjustment.basic().exposure() <= 1.5, issues, "exposure 在安全范围内", "exposure 超出 ±1.5");
        checkRange(adjustment.basic().contrast(), -100, 100, "contrast", issues);
        checkRange(adjustment.basic().highlights(), -100, 100, "highlights", issues);
        checkRange(adjustment.basic().shadows(), -100, 100, "shadows", issues);
        checkRange(adjustment.basic().whites(), -100, 100, "whites", issues);
        checkRange(adjustment.basic().blacks(), -100, 100, "blacks", issues);
        check(isTemperatureInRange(adjustment.basic().temperature()), issues, "temperature 在范围内", "temperature 超出范围");
        checkRange(adjustment.basic().tint(), -50, 50, "tint", issues);
        checkRange(adjustment.basic().texture(), -100, 100, "texture", issues);
        checkRange(adjustment.basic().clarity(), -100, 100, "clarity", issues);
        checkRange(adjustment.basic().dehaze(), -100, 100, "dehaze", issues);
        checkRange(adjustment.basic().vibrance(), -100, 100, "vibrance", issues);
        checkRange(adjustment.basic().saturation(), -100, 100, "saturation", issues);
        validateHsl(adjustment, issues);
        checkRange(adjustment.effects().grain(), 0, 100, "grain", issues);
        checkRange(adjustment.effects().vignette(), -100, 100, "vignette", issues);
        return issues;
    }

    private void validateHsl(ColorAdjustment adjustment, List<String> issues) {
        checkRange(adjustment.hsl().redHue(), -100, 100, "redHue", issues);
        checkRange(adjustment.hsl().redSaturation(), -100, 100, "redSaturation", issues);
        checkRange(adjustment.hsl().redLuminance(), -100, 100, "redLuminance", issues);
        checkRange(adjustment.hsl().orangeHue(), -100, 100, "orangeHue", issues);
        checkRange(adjustment.hsl().orangeSaturation(), -100, 100, "orangeSaturation", issues);
        checkRange(adjustment.hsl().orangeLuminance(), -100, 100, "orangeLuminance", issues);
        checkRange(adjustment.hsl().yellowHue(), -100, 100, "yellowHue", issues);
        checkRange(adjustment.hsl().yellowSaturation(), -100, 100, "yellowSaturation", issues);
        checkRange(adjustment.hsl().yellowLuminance(), -100, 100, "yellowLuminance", issues);
        checkRange(adjustment.hsl().greenHue(), -100, 100, "greenHue", issues);
        checkRange(adjustment.hsl().greenSaturation(), -100, 100, "greenSaturation", issues);
        checkRange(adjustment.hsl().greenLuminance(), -100, 100, "greenLuminance", issues);
        checkRange(adjustment.hsl().aquaHue(), -100, 100, "aquaHue", issues);
        checkRange(adjustment.hsl().aquaSaturation(), -100, 100, "aquaSaturation", issues);
        checkRange(adjustment.hsl().aquaLuminance(), -100, 100, "aquaLuminance", issues);
        checkRange(adjustment.hsl().blueHue(), -100, 100, "blueHue", issues);
        checkRange(adjustment.hsl().blueSaturation(), -100, 100, "blueSaturation", issues);
        checkRange(adjustment.hsl().blueLuminance(), -100, 100, "blueLuminance", issues);
        checkRange(adjustment.hsl().purpleHue(), -100, 100, "purpleHue", issues);
        checkRange(adjustment.hsl().purpleSaturation(), -100, 100, "purpleSaturation", issues);
        checkRange(adjustment.hsl().purpleLuminance(), -100, 100, "purpleLuminance", issues);
        checkRange(adjustment.hsl().magentaHue(), -100, 100, "magentaHue", issues);
        checkRange(adjustment.hsl().magentaSaturation(), -100, 100, "magentaSaturation", issues);
        checkRange(adjustment.hsl().magentaLuminance(), -100, 100, "magentaLuminance", issues);
    }

    private void checkRange(int value, int min, int max, String field, List<String> issues) {
        check(value >= min && value <= max, issues, field + " 在范围内", field + " 超出范围");
    }

    private boolean isTemperatureInRange(int value) {
        if (Math.abs(value) >= 1000) {
            return value >= 2000 && value <= 50000;
        }
        return value >= -50 && value <= 50;
    }

    private void check(boolean passed, List<String> issues, String ok, String fail) {
        issues.add(passed ? ok : fail);
    }
}
