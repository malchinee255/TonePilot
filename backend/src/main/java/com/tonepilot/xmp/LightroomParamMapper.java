package com.tonepilot.xmp;

import com.tonepilot.domain.ColorAdjustment;

import java.util.LinkedHashMap;
import java.util.Map;

public class LightroomParamMapper {

    public Map<String, String> toXmpAttributes(ColorAdjustment adjustment) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("crs:Exposure2012", formatDouble(adjustment.basic().exposure()));
        values.put("crs:Contrast2012", formatInt(adjustment.basic().contrast()));
        values.put("crs:Highlights2012", formatInt(adjustment.basic().highlights()));
        values.put("crs:Shadows2012", formatInt(adjustment.basic().shadows()));
        values.put("crs:Whites2012", formatInt(adjustment.basic().whites()));
        values.put("crs:Blacks2012", formatInt(adjustment.basic().blacks()));
        values.put("crs:Temperature", formatInt(adjustment.basic().temperature()));
        values.put("crs:Tint", formatInt(adjustment.basic().tint()));
        values.put("crs:Texture", formatInt(adjustment.basic().texture()));
        values.put("crs:Clarity2012", formatInt(adjustment.basic().clarity()));
        values.put("crs:Dehaze", formatInt(adjustment.basic().dehaze()));
        values.put("crs:Vibrance", formatInt(adjustment.basic().vibrance()));
        values.put("crs:Saturation", formatInt(adjustment.basic().saturation()));

        values.put("crs:HueAdjustmentRed", formatInt(adjustment.hsl().redHue()));
        values.put("crs:HueAdjustmentOrange", formatInt(adjustment.hsl().orangeHue()));
        values.put("crs:HueAdjustmentYellow", formatInt(adjustment.hsl().yellowHue()));
        values.put("crs:HueAdjustmentGreen", formatInt(adjustment.hsl().greenHue()));
        values.put("crs:HueAdjustmentAqua", formatInt(adjustment.hsl().aquaHue()));
        values.put("crs:HueAdjustmentBlue", formatInt(adjustment.hsl().blueHue()));
        values.put("crs:HueAdjustmentPurple", formatInt(adjustment.hsl().purpleHue()));
        values.put("crs:HueAdjustmentMagenta", formatInt(adjustment.hsl().magentaHue()));

        values.put("crs:SaturationAdjustmentRed", formatInt(adjustment.hsl().redSaturation()));
        values.put("crs:SaturationAdjustmentOrange", formatInt(adjustment.hsl().orangeSaturation()));
        values.put("crs:SaturationAdjustmentYellow", formatInt(adjustment.hsl().yellowSaturation()));
        values.put("crs:SaturationAdjustmentGreen", formatInt(adjustment.hsl().greenSaturation()));
        values.put("crs:SaturationAdjustmentAqua", formatInt(adjustment.hsl().aquaSaturation()));
        values.put("crs:SaturationAdjustmentBlue", formatInt(adjustment.hsl().blueSaturation()));
        values.put("crs:SaturationAdjustmentPurple", formatInt(adjustment.hsl().purpleSaturation()));
        values.put("crs:SaturationAdjustmentMagenta", formatInt(adjustment.hsl().magentaSaturation()));

        values.put("crs:LuminanceAdjustmentRed", formatInt(adjustment.hsl().redLuminance()));
        values.put("crs:LuminanceAdjustmentOrange", formatInt(adjustment.hsl().orangeLuminance()));
        values.put("crs:LuminanceAdjustmentYellow", formatInt(adjustment.hsl().yellowLuminance()));
        values.put("crs:LuminanceAdjustmentGreen", formatInt(adjustment.hsl().greenLuminance()));
        values.put("crs:LuminanceAdjustmentAqua", formatInt(adjustment.hsl().aquaLuminance()));
        values.put("crs:LuminanceAdjustmentBlue", formatInt(adjustment.hsl().blueLuminance()));
        values.put("crs:LuminanceAdjustmentPurple", formatInt(adjustment.hsl().purpleLuminance()));
        values.put("crs:LuminanceAdjustmentMagenta", formatInt(adjustment.hsl().magentaLuminance()));

        values.put("crs:GrainAmount", formatInt(adjustment.effects().grain()));
        values.put("crs:PostCropVignetteAmount", formatInt(adjustment.effects().vignette()));
        return values;
    }

    private String formatInt(int value) {
        return Integer.toString(value);
    }

    private String formatDouble(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
