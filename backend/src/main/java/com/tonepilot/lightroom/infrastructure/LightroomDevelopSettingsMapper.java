package com.tonepilot.lightroom.infrastructure;

import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;
import com.tonepilot.lightroom.domain.ParameterDelta;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LightroomDevelopSettingsMapper {

    private static final Map<String, String> EXTENDED_SETTINGS = createExtendedSettings();

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
        putExtendedSettings(settings, adjustment);
        return settings;
    }

    public Map<String, Object> toDevelopSettings(ColorAdjustment adjustment, List<ParameterDelta> deltas) {
        Map<String, Object> settings = new LinkedHashMap<>();
        if (deltas == null || deltas.isEmpty()) {
            return settings;
        }
        for (ParameterDelta delta : deltas) {
            putDeltaSetting(settings, adjustment, delta.name());
        }
        return settings;
    }

    private void putDeltaSetting(Map<String, Object> settings, ColorAdjustment adjustment, String name) {
        LightroomBasicParams basic = adjustment.basic();
        LightroomHslParams hsl = adjustment.hsl();
        LightroomEffectsParams effects = adjustment.effects();
        switch (name) {
            case "exposure" -> settings.put("Exposure2012", basic.exposure());
            case "contrast" -> settings.put("Contrast2012", basic.contrast());
            case "highlights" -> settings.put("Highlights2012", basic.highlights());
            case "shadows" -> settings.put("Shadows2012", basic.shadows());
            case "whites" -> settings.put("Whites2012", basic.whites());
            case "blacks" -> settings.put("Blacks2012", basic.blacks());
            case "temperature" -> settings.put("Temperature", basic.temperature());
            case "tint" -> settings.put("Tint", basic.tint());
            case "texture" -> settings.put("Texture", basic.texture());
            case "clarity" -> settings.put("Clarity2012", basic.clarity());
            case "dehaze" -> settings.put("Dehaze", basic.dehaze());
            case "vibrance" -> settings.put("Vibrance", basic.vibrance());
            case "saturation" -> settings.put("Saturation", basic.saturation());
            case "redHue" -> settings.put("HueAdjustmentRed", hsl.redHue());
            case "redSaturation" -> settings.put("SaturationAdjustmentRed", hsl.redSaturation());
            case "redLuminance" -> settings.put("LuminanceAdjustmentRed", hsl.redLuminance());
            case "orangeHue" -> settings.put("HueAdjustmentOrange", hsl.orangeHue());
            case "orangeSaturation" -> settings.put("SaturationAdjustmentOrange", hsl.orangeSaturation());
            case "orangeLuminance" -> settings.put("LuminanceAdjustmentOrange", hsl.orangeLuminance());
            case "yellowHue" -> settings.put("HueAdjustmentYellow", hsl.yellowHue());
            case "yellowSaturation" -> settings.put("SaturationAdjustmentYellow", hsl.yellowSaturation());
            case "yellowLuminance" -> settings.put("LuminanceAdjustmentYellow", hsl.yellowLuminance());
            case "greenHue" -> settings.put("HueAdjustmentGreen", hsl.greenHue());
            case "greenSaturation" -> settings.put("SaturationAdjustmentGreen", hsl.greenSaturation());
            case "greenLuminance" -> settings.put("LuminanceAdjustmentGreen", hsl.greenLuminance());
            case "aquaHue" -> settings.put("HueAdjustmentAqua", hsl.aquaHue());
            case "aquaSaturation" -> settings.put("SaturationAdjustmentAqua", hsl.aquaSaturation());
            case "aquaLuminance" -> settings.put("LuminanceAdjustmentAqua", hsl.aquaLuminance());
            case "blueHue" -> settings.put("HueAdjustmentBlue", hsl.blueHue());
            case "blueSaturation" -> settings.put("SaturationAdjustmentBlue", hsl.blueSaturation());
            case "blueLuminance" -> settings.put("LuminanceAdjustmentBlue", hsl.blueLuminance());
            case "purpleHue" -> settings.put("HueAdjustmentPurple", hsl.purpleHue());
            case "purpleSaturation" -> settings.put("SaturationAdjustmentPurple", hsl.purpleSaturation());
            case "purpleLuminance" -> settings.put("LuminanceAdjustmentPurple", hsl.purpleLuminance());
            case "magentaHue" -> settings.put("HueAdjustmentMagenta", hsl.magentaHue());
            case "magentaSaturation" -> settings.put("SaturationAdjustmentMagenta", hsl.magentaSaturation());
            case "magentaLuminance" -> settings.put("LuminanceAdjustmentMagenta", hsl.magentaLuminance());
            case "grain" -> settings.put("GrainAmount", effects.grain());
            case "vignette" -> settings.put("PostCropVignetteAmount", effects.vignette());
            default -> putExtendedSetting(settings, adjustment, name);
        }
    }

    private void putExtendedSettings(Map<String, Object> settings, ColorAdjustment adjustment) {
        if (adjustment.extended() == null || adjustment.extended().isEmpty()) {
            return;
        }
        for (String name : adjustment.extended().keySet()) {
            putExtendedSetting(settings, adjustment, name);
        }
    }

    private void putExtendedSetting(Map<String, Object> settings, ColorAdjustment adjustment, String name) {
        String lightroomName = EXTENDED_SETTINGS.get(name);
        if (lightroomName == null || adjustment.extended() == null || !adjustment.extended().containsKey(name)) {
            return;
        }
        Object value = adjustment.extended().get(name);
        if (value != null) {
            settings.put(lightroomName, value);
        }
    }

    private static Map<String, String> createExtendedSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("processVersion", "ProcessVersion");
        settings.put("treatment", "Treatment");
        settings.put("profileName", "ProfileName");

        settings.put("parametricShadows", "ParametricShadows");
        settings.put("parametricDarks", "ParametricDarks");
        settings.put("parametricLights", "ParametricLights");
        settings.put("parametricHighlights", "ParametricHighlights");
        settings.put("parametricShadowSplit", "ParametricShadowSplit");
        settings.put("parametricMidtoneSplit", "ParametricMidtoneSplit");
        settings.put("parametricHighlightSplit", "ParametricHighlightSplit");
        settings.put("toneCurveName2012", "ToneCurveName2012");
        settings.put("toneCurvePV2012", "ToneCurvePV2012");
        settings.put("toneCurvePV2012Red", "ToneCurvePV2012Red");
        settings.put("toneCurvePV2012Green", "ToneCurvePV2012Green");
        settings.put("toneCurvePV2012Blue", "ToneCurvePV2012Blue");

        settings.put("colorGradeShadowHue", "ColorGradeShadowHue");
        settings.put("colorGradeShadowSat", "ColorGradeShadowSat");
        settings.put("colorGradeMidtoneHue", "ColorGradeMidtoneHue");
        settings.put("colorGradeMidtoneSat", "ColorGradeMidtoneSat");
        settings.put("colorGradeHighlightHue", "ColorGradeHighlightHue");
        settings.put("colorGradeHighlightSat", "ColorGradeHighlightSat");
        settings.put("colorGradeBlending", "ColorGradeBlending");
        settings.put("colorGradeGlobalHue", "ColorGradeGlobalHue");
        settings.put("colorGradeGlobalSat", "ColorGradeGlobalSat");
        settings.put("splitToningShadowHue", "SplitToningShadowHue");
        settings.put("splitToningShadowSaturation", "SplitToningShadowSaturation");
        settings.put("splitToningHighlightHue", "SplitToningHighlightHue");
        settings.put("splitToningHighlightSaturation", "SplitToningHighlightSaturation");
        settings.put("splitToningBalance", "SplitToningBalance");

        settings.put("sharpness", "Sharpness");
        settings.put("sharpenRadius", "SharpenRadius");
        settings.put("sharpenDetail", "SharpenDetail");
        settings.put("sharpenEdgeMasking", "SharpenEdgeMasking");
        settings.put("luminanceSmoothing", "LuminanceSmoothing");
        settings.put("luminanceNoiseReductionDetail", "LuminanceNoiseReductionDetail");
        settings.put("luminanceNoiseReductionContrast", "LuminanceNoiseReductionContrast");
        settings.put("colorNoiseReduction", "ColorNoiseReduction");
        settings.put("colorNoiseReductionDetail", "ColorNoiseReductionDetail");
        settings.put("colorNoiseReductionSmoothness", "ColorNoiseReductionSmoothness");

        settings.put("lensProfileEnable", "LensProfileEnable");
        settings.put("lensManualDistortionAmount", "LensManualDistortionAmount");
        settings.put("perspectiveVertical", "PerspectiveVertical");
        settings.put("perspectiveHorizontal", "PerspectiveHorizontal");
        settings.put("perspectiveRotate", "PerspectiveRotate");
        settings.put("perspectiveScale", "PerspectiveScale");
        settings.put("perspectiveAspect", "PerspectiveAspect");
        settings.put("perspectiveX", "PerspectiveX");
        settings.put("perspectiveY", "PerspectiveY");
        settings.put("uprightTransformMode", "UprightTransformMode");
        settings.put("autoLateralCA", "AutoLateralCA");
        settings.put("defringePurpleAmount", "DefringePurpleAmount");
        settings.put("defringePurpleHueLo", "DefringePurpleHueLo");
        settings.put("defringePurpleHueHi", "DefringePurpleHueHi");
        settings.put("defringeGreenAmount", "DefringeGreenAmount");
        settings.put("defringeGreenHueLo", "DefringeGreenHueLo");
        settings.put("defringeGreenHueHi", "DefringeGreenHueHi");

        settings.put("postCropVignetteStyle", "PostCropVignetteStyle");
        settings.put("postCropVignetteMidpoint", "PostCropVignetteMidpoint");
        settings.put("postCropVignetteRoundness", "PostCropVignetteRoundness");
        settings.put("postCropVignetteFeather", "PostCropVignetteFeather");
        settings.put("postCropVignetteHighlightContrast", "PostCropVignetteHighlightContrast");
        settings.put("grainSize", "GrainSize");
        settings.put("grainFrequency", "GrainFrequency");

        settings.put("redPrimaryHue", "RedPrimaryHue");
        settings.put("redPrimarySaturation", "RedPrimarySaturation");
        settings.put("greenPrimaryHue", "GreenPrimaryHue");
        settings.put("greenPrimarySaturation", "GreenPrimarySaturation");
        settings.put("bluePrimaryHue", "BluePrimaryHue");
        settings.put("bluePrimarySaturation", "BluePrimarySaturation");
        settings.put("shadowTint", "ShadowTint");

        settings.put("cropTop", "CropTop");
        settings.put("cropLeft", "CropLeft");
        settings.put("cropBottom", "CropBottom");
        settings.put("cropRight", "CropRight");
        settings.put("cropAngle", "CropAngle");
        settings.put("orientation", "Orientation");
        return Map.copyOf(settings);
    }
}
