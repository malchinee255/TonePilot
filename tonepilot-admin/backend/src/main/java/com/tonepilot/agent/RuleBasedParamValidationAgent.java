package com.tonepilot.agent;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;
import com.tonepilot.harness.ParamRangeValidator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class RuleBasedParamValidationAgent implements ParamValidationAgent {

    private final ParamRangeValidator rangeValidator;

    @Autowired
    public RuleBasedParamValidationAgent(ParamRangeValidator rangeValidator) {
        this.rangeValidator = rangeValidator;
    }

    @Override
    public ParamValidationResult validate(ColorAdjustment adjustment) {
        ColorAdjustment normalized = normalizeMissingParts(adjustment);
        List<String> messages = new ArrayList<>(rangeValidator.validate(normalized));
        LightroomBasicParams basic = safeBasic(normalized.basic());
        LightroomHslParams hsl = safeHsl(normalized.hsl());
        LightroomEffectsParams effects = safeEffects(normalized.effects());

        boolean corrected = !Objects.equals(basic, normalized.basic())
                || !Objects.equals(hsl, normalized.hsl())
                || !Objects.equals(effects, normalized.effects());
        if (corrected) {
            messages.add("已自动将越界参数收敛到安全范围");
        }

        Map<String, Object> rawResponse = new LinkedHashMap<>();
        if (normalized.rawResponse() != null) {
            rawResponse.putAll(normalized.rawResponse());
        }
        rawResponse.put("validationMessages", messages);
        rawResponse.put("validationCorrected", corrected);

        ColorAdjustment safeAdjustment = new ColorAdjustment(
                normalized.id(),
                normalized.photoId(),
                normalized.style(),
                normalized.reason(),
                basic,
                hsl,
                effects,
                normalized.extended(),
                normalized.steps() == null ? List.of() : normalized.steps(),
                rawResponse,
                normalized.createdAt() == null ? Instant.now() : normalized.createdAt()
        );
        return new ParamValidationResult(safeAdjustment, messages, corrected);
    }

    private ColorAdjustment normalizeMissingParts(ColorAdjustment adjustment) {
        if (adjustment.basic() != null && adjustment.hsl() != null && adjustment.effects() != null) {
            return adjustment;
        }
        return new ColorAdjustment(
                adjustment.id(),
                adjustment.photoId(),
                adjustment.style(),
                adjustment.reason(),
                adjustment.basic() == null ? defaultBasic() : adjustment.basic(),
                adjustment.hsl() == null ? defaultHsl() : adjustment.hsl(),
                adjustment.effects() == null ? new LightroomEffectsParams(0, 0) : adjustment.effects(),
                adjustment.extended(),
                adjustment.steps(),
                adjustment.rawResponse(),
                adjustment.createdAt()
        );
    }

    private LightroomBasicParams safeBasic(LightroomBasicParams value) {
        return new LightroomBasicParams(
                clamp(value.exposure(), -1.5, 1.5),
                clamp(value.contrast(), -100, 100),
                clamp(value.highlights(), -100, 100),
                clamp(value.shadows(), -100, 100),
                clamp(value.whites(), -100, 100),
                clamp(value.blacks(), -100, 100),
                clampTemperature(value.temperature()),
                clamp(value.tint(), -50, 50),
                clamp(value.texture(), -100, 100),
                clamp(value.clarity(), -100, 100),
                clamp(value.dehaze(), -100, 100),
                clamp(value.vibrance(), -100, 100),
                clamp(value.saturation(), -100, 100)
        );
    }

    private LightroomHslParams safeHsl(LightroomHslParams value) {
        return new LightroomHslParams(
                clamp(value.redHue(), -100, 100),
                clamp(value.redSaturation(), -100, 100),
                clamp(value.redLuminance(), -100, 100),
                clamp(value.orangeHue(), -100, 100),
                clamp(value.orangeSaturation(), -100, 100),
                clamp(value.orangeLuminance(), -100, 100),
                clamp(value.yellowHue(), -100, 100),
                clamp(value.yellowSaturation(), -100, 100),
                clamp(value.yellowLuminance(), -100, 100),
                clamp(value.greenHue(), -100, 100),
                clamp(value.greenSaturation(), -100, 100),
                clamp(value.greenLuminance(), -100, 100),
                clamp(value.aquaHue(), -100, 100),
                clamp(value.aquaSaturation(), -100, 100),
                clamp(value.aquaLuminance(), -100, 100),
                clamp(value.blueHue(), -100, 100),
                clamp(value.blueSaturation(), -100, 100),
                clamp(value.blueLuminance(), -100, 100),
                clamp(value.purpleHue(), -100, 100),
                clamp(value.purpleSaturation(), -100, 100),
                clamp(value.purpleLuminance(), -100, 100),
                clamp(value.magentaHue(), -100, 100),
                clamp(value.magentaSaturation(), -100, 100),
                clamp(value.magentaLuminance(), -100, 100)
        );
    }

    private LightroomEffectsParams safeEffects(LightroomEffectsParams value) {
        return new LightroomEffectsParams(
                clamp(value.grain(), 0, 100),
                clamp(value.vignette(), -100, 100)
        );
    }

    private LightroomBasicParams defaultBasic() {
        return new LightroomBasicParams(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private LightroomHslParams defaultHsl() {
        return new LightroomHslParams(
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0
        );
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampTemperature(int value) {
        if (Math.abs(value) >= 1000) {
            return clamp(value, 2000, 50000);
        }
        return clamp(value, -50, 50);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


