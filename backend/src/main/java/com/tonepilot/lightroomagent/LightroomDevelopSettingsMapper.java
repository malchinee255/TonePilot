package com.tonepilot.lightroomagent;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LightroomDevelopSettingsMapper {

    public Map<String, Object> toDevelopSettings(ColorAdjustment adjustment) {
        LightroomBasicParams basic = adjustment.basic();
        LightroomHslParams hsl = adjustment.hsl();
        LightroomEffectsParams effects = adjustment.effects();
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("Exposure2012", basic.exposure());
        settings.put("Contrast2012", basic.contrast());
        settings.put("Highlights2012", basic.highlights());
        settings.put("Shadows2012", basic.shadows());
        settings.put("Whites2012", basic.whites());
        settings.put("Blacks2012", basic.blacks());
        settings.put("Temperature", basic.temperature());
        settings.put("Tint", basic.tint());
        settings.put("Texture", basic.texture());
        settings.put("Clarity2012", basic.clarity());
        settings.put("Dehaze", basic.dehaze());
        settings.put("Vibrance", basic.vibrance());
        settings.put("Saturation", basic.saturation());
        settings.put("HueAdjustmentRed", hsl.redHue());
        settings.put("SaturationAdjustmentRed", hsl.redSaturation());
        settings.put("LuminanceAdjustmentRed", hsl.redLuminance());
        settings.put("HueAdjustmentOrange", hsl.orangeHue());
        settings.put("SaturationAdjustmentOrange", hsl.orangeSaturation());
        settings.put("LuminanceAdjustmentOrange", hsl.orangeLuminance());
        settings.put("HueAdjustmentYellow", hsl.yellowHue());
        settings.put("SaturationAdjustmentYellow", hsl.yellowSaturation());
        settings.put("LuminanceAdjustmentYellow", hsl.yellowLuminance());
        settings.put("HueAdjustmentGreen", hsl.greenHue());
        settings.put("SaturationAdjustmentGreen", hsl.greenSaturation());
        settings.put("LuminanceAdjustmentGreen", hsl.greenLuminance());
        settings.put("HueAdjustmentAqua", hsl.aquaHue());
        settings.put("SaturationAdjustmentAqua", hsl.aquaSaturation());
        settings.put("LuminanceAdjustmentAqua", hsl.aquaLuminance());
        settings.put("HueAdjustmentBlue", hsl.blueHue());
        settings.put("SaturationAdjustmentBlue", hsl.blueSaturation());
        settings.put("LuminanceAdjustmentBlue", hsl.blueLuminance());
        settings.put("HueAdjustmentPurple", hsl.purpleHue());
        settings.put("SaturationAdjustmentPurple", hsl.purpleSaturation());
        settings.put("LuminanceAdjustmentPurple", hsl.purpleLuminance());
        settings.put("HueAdjustmentMagenta", hsl.magentaHue());
        settings.put("SaturationAdjustmentMagenta", hsl.magentaSaturation());
        settings.put("LuminanceAdjustmentMagenta", hsl.magentaLuminance());
        settings.put("GrainAmount", effects.grain());
        settings.put("PostCropVignetteAmount", effects.vignette());
        return settings;
    }
}
