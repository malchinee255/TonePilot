package com.tonepilot.lightroomagent;

import com.tonepilot.agent.ParamValidationAgent;
import com.tonepilot.agent.ParamValidationResult;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Component
public class TuningAdjustmentPlanner {

    private final ParamValidationAgent paramValidationAgent;

    public TuningAdjustmentPlanner(ParamValidationAgent paramValidationAgent) {
        this.paramValidationAgent = paramValidationAgent;
    }

    public TuningPlan apply(ColorAdjustment source, String message) {
        String text = normalize(message);
        source = normalizeSource(source);
        MutableAdjustment mutable = MutableAdjustment.from(source);
        List<ParameterDelta> requestedDeltas = new ArrayList<>();

        if (containsAny(text, "亮一点", "更亮", "提亮", "亮一些", "太暗")) {
            mutable.exposure += 0.18;
            mutable.shadows += 8;
            requestedDeltas.add(delta("basic", "exposure", "曝光", source.basic().exposure(), mutable.exposure, "+0.18", "用户希望画面更亮"));
            requestedDeltas.add(delta("basic", "shadows", "阴影", source.basic().shadows(), mutable.shadows, "+8", "提亮暗部细节"));
        }
        if (containsAny(text, "暗一点", "更暗", "压暗", "太亮")) {
            mutable.exposure -= 0.18;
            mutable.highlights -= 8;
            requestedDeltas.add(delta("basic", "exposure", "曝光", source.basic().exposure(), mutable.exposure, "-0.18", "用户希望画面更暗"));
            requestedDeltas.add(delta("basic", "highlights", "高光", source.basic().highlights(), mutable.highlights, "-8", "压住高光亮度"));
        }
        if (containsAny(text, "暖一点", "更暖", "偏暖", "暖调")) {
            mutable.temperature += 6;
            requestedDeltas.add(delta("basic", "temperature", "色温", source.basic().temperature(), mutable.temperature, "+6", "用户希望白平衡更暖"));
        }
        if (containsAny(text, "冷一点", "更冷", "偏冷", "冷调")) {
            mutable.temperature -= 6;
            requestedDeltas.add(delta("basic", "temperature", "色温", source.basic().temperature(), mutable.temperature, "-6", "用户希望白平衡更冷"));
        }
        if (containsAny(text, "对比", "层次", "立体")) {
            mutable.contrast += 8;
            mutable.clarity += 4;
            requestedDeltas.add(delta("basic", "contrast", "对比度", source.basic().contrast(), mutable.contrast, "+8", "增强明暗层次"));
            requestedDeltas.add(delta("basic", "clarity", "清晰度", source.basic().clarity(), mutable.clarity, "+4", "增强中间调质感"));
        }
        if (containsAny(text, "柔和", "淡一点", "低对比")) {
            mutable.contrast -= 6;
            mutable.clarity -= 3;
            requestedDeltas.add(delta("basic", "contrast", "对比度", source.basic().contrast(), mutable.contrast, "-6", "降低明暗反差"));
            requestedDeltas.add(delta("basic", "clarity", "清晰度", source.basic().clarity(), mutable.clarity, "-3", "让画面更柔和"));
        }
        if (containsAny(text, "饱和", "鲜艳", "浓一点")) {
            mutable.vibrance += 8;
            mutable.saturation += 3;
            requestedDeltas.add(delta("basic", "vibrance", "自然饱和度", source.basic().vibrance(), mutable.vibrance, "+8", "增强整体色彩活力"));
            requestedDeltas.add(delta("basic", "saturation", "饱和度", source.basic().saturation(), mutable.saturation, "+3", "轻微增强颜色浓度"));
        }
        if (containsAny(text, "低饱和", "褪色", "淡雅", "颜色太重")) {
            mutable.vibrance -= 6;
            mutable.saturation -= 6;
            requestedDeltas.add(delta("basic", "vibrance", "自然饱和度", source.basic().vibrance(), mutable.vibrance, "-6", "降低整体色彩刺激感"));
            requestedDeltas.add(delta("basic", "saturation", "饱和度", source.basic().saturation(), mutable.saturation, "-6", "降低颜色浓度"));
        }
        if (containsAny(text, "肤色", "人像")) {
            mutable.orangeSaturation += 4;
            mutable.orangeLuminance += 4;
            requestedDeltas.add(delta("hsl", "orangeSaturation", "橙色饱和度", source.hsl().orangeSaturation(), mutable.orangeSaturation, "+4", "优化肤色气色"));
            requestedDeltas.add(delta("hsl", "orangeLuminance", "橙色明亮度", source.hsl().orangeLuminance(), mutable.orangeLuminance, "+4", "让肤色更通透"));
        }
        if (containsAny(text, "绿色脏", "绿太重", "降低绿色", "草地")) {
            mutable.greenSaturation -= 10;
            mutable.greenLuminance += 4;
            requestedDeltas.add(delta("hsl", "greenSaturation", "绿色饱和度", source.hsl().greenSaturation(), mutable.greenSaturation, "-10", "压低偏脏绿色"));
            requestedDeltas.add(delta("hsl", "greenLuminance", "绿色明亮度", source.hsl().greenLuminance(), mutable.greenLuminance, "+4", "让绿色更干净"));
        }
        if (containsAny(text, "天空", "蓝色", "海水")) {
            mutable.blueSaturation += 6;
            mutable.blueLuminance -= 4;
            requestedDeltas.add(delta("hsl", "blueSaturation", "蓝色饱和度", source.hsl().blueSaturation(), mutable.blueSaturation, "+6", "增强蓝色主体"));
            requestedDeltas.add(delta("hsl", "blueLuminance", "蓝色明亮度", source.hsl().blueLuminance(), mutable.blueLuminance, "-4", "压出天空或海水层次"));
        }
        if (containsAny(text, "颗粒", "胶片颗粒")) {
            mutable.grain += 10;
            requestedDeltas.add(delta("effects", "grain", "颗粒", source.effects().grain(), mutable.grain, "+10", "增加胶片颗粒感"));
        }
        if (containsAny(text, "暗角", "边缘压暗")) {
            mutable.vignette -= 10;
            requestedDeltas.add(delta("effects", "vignette", "暗角", source.effects().vignette(), mutable.vignette, "-10", "压暗画面边缘聚焦主体"));
        }
        if (containsAny(text, "电影感", "cinematic", "赛博", "氛围感")) {
            mutable.contrast += 10;
            mutable.highlights -= 14;
            mutable.shadows += 6;
            mutable.dehaze += 5;
            mutable.vignette -= 8;
            requestedDeltas.add(delta("basic", "contrast", "对比度", source.basic().contrast(), mutable.contrast, "+10", "增强电影感明暗结构"));
            requestedDeltas.add(delta("basic", "highlights", "高光", source.basic().highlights(), mutable.highlights, "-14", "压住亮部并保留灯光细节"));
            requestedDeltas.add(delta("basic", "shadows", "阴影", source.basic().shadows(), mutable.shadows, "+6", "保留暗部可读性"));
            requestedDeltas.add(delta("basic", "dehaze", "去朦胧", source.basic().dehaze(), mutable.dehaze, "+5", "增加画面空气透视和层次"));
            requestedDeltas.add(delta("effects", "vignette", "暗角", source.effects().vignette(), mutable.vignette, "-8", "轻微压暗边缘形成视线聚焦"));
        }
        if (containsAny(text, "日系", "清透", "干净")) {
            mutable.exposure += 0.12;
            mutable.contrast -= 6;
            mutable.highlights -= 8;
            mutable.shadows += 10;
            mutable.vibrance += 4;
            mutable.greenSaturation -= 8;
            requestedDeltas.add(delta("basic", "exposure", "曝光", source.basic().exposure(), mutable.exposure, "+0.12", "提升整体清透感"));
            requestedDeltas.add(delta("basic", "contrast", "对比度", source.basic().contrast(), mutable.contrast, "-6", "降低硬反差"));
            requestedDeltas.add(delta("basic", "highlights", "高光", source.basic().highlights(), mutable.highlights, "-8", "避免亮部发灰或溢出"));
            requestedDeltas.add(delta("basic", "shadows", "阴影", source.basic().shadows(), mutable.shadows, "+10", "提亮暗部让画面更轻盈"));
            requestedDeltas.add(delta("basic", "vibrance", "自然饱和度", source.basic().vibrance(), mutable.vibrance, "+4", "保留柔和色彩活力"));
            requestedDeltas.add(delta("hsl", "greenSaturation", "绿色饱和度", source.hsl().greenSaturation(), mutable.greenSaturation, "-8", "降低绿色干扰"));
        }
        if (containsAny(text, "胶片", "film", "复古")) {
            mutable.contrast += 6;
            mutable.highlights -= 8;
            mutable.blacks += 6;
            mutable.grain += 12;
            mutable.vignette -= 6;
            mutable.saturation -= 4;
            requestedDeltas.add(delta("basic", "contrast", "对比度", source.basic().contrast(), mutable.contrast, "+6", "建立胶片式明暗骨架"));
            requestedDeltas.add(delta("basic", "highlights", "高光", source.basic().highlights(), mutable.highlights, "-8", "压住数码高光"));
            requestedDeltas.add(delta("basic", "blacks", "黑色色阶", source.basic().blacks(), mutable.blacks, "+6", "抬黑形成轻微褪色感"));
            requestedDeltas.add(delta("effects", "grain", "颗粒", source.effects().grain(), mutable.grain, "+12", "增加胶片颗粒"));
            requestedDeltas.add(delta("effects", "vignette", "暗角", source.effects().vignette(), mutable.vignette, "-6", "增加镜头边缘氛围"));
            requestedDeltas.add(delta("basic", "saturation", "饱和度", source.basic().saturation(), mutable.saturation, "-4", "降低数码感"));
        }

        if (requestedDeltas.isEmpty()) {
            return new TuningPlan(source, List.of(), "没有识别到明确的调色动作，可以试试“再亮一点”“暖一点”“降低绿色”这类指令。");
        }

        ColorAdjustment candidate = mutable.toAdjustment(source);
        ParamValidationResult validation = paramValidationAgent.validate(candidate);
        List<ParameterDelta> finalDeltas = AdjustmentDiffer.diff(source, validation.adjustment(), requestedDeltas);
        String assistantMessage = "已根据你的描述微调 " + finalDeltas.size() + " 个参数：" +
                String.join("、", finalDeltas.stream().map(ParameterDelta::label).toList()) + "。";
        return new TuningPlan(validation.adjustment(), finalDeltas, assistantMessage);
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
    }

    private ColorAdjustment normalizeSource(ColorAdjustment source) {
        return new ColorAdjustment(
                source.id(),
                source.photoId(),
                source.style(),
                source.reason(),
                source.basic() == null ? MutableAdjustment.defaultBasic() : source.basic(),
                source.hsl() == null ? MutableAdjustment.defaultHsl() : source.hsl(),
                source.effects() == null ? MutableAdjustment.defaultEffects() : source.effects(),
                source.steps(),
                source.rawResponse(),
                source.createdAt()
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private ParameterDelta delta(String group, String name, String label, double before, double after, String delta, String reason) {
        return new ParameterDelta(group, name, label, format(before), format(after), delta, reason);
    }

    private ParameterDelta delta(String group, String name, String label, int before, int after, String delta, String reason) {
        return new ParameterDelta(group, name, label, String.valueOf(before), String.valueOf(after), delta, reason);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static class AdjustmentDiffer {

        private static List<ParameterDelta> diff(ColorAdjustment before, ColorAdjustment after, List<ParameterDelta> requestedDeltas) {
            LinkedHashMap<String, ParameterDelta> result = new LinkedHashMap<>();
            for (ParameterDelta requested : requestedDeltas) {
                Object beforeValue = read(before, requested.name());
                Object afterValue = read(after, requested.name());
                if (beforeValue != null && afterValue != null && !beforeValue.equals(afterValue)) {
                    result.put(requested.name(), new ParameterDelta(
                            requested.group(),
                            requested.name(),
                            requested.label(),
                            formatValue(beforeValue),
                            formatValue(afterValue),
                            deltaValue(beforeValue, afterValue),
                            requested.reason()
                    ));
                }
            }
            return List.copyOf(result.values());
        }

        private static Object read(ColorAdjustment adjustment, String name) {
            LightroomBasicParams basic = adjustment.basic();
            LightroomHslParams hsl = adjustment.hsl();
            LightroomEffectsParams effects = adjustment.effects();
            return switch (name) {
                case "exposure" -> basic.exposure();
                case "contrast" -> basic.contrast();
                case "highlights" -> basic.highlights();
                case "shadows" -> basic.shadows();
                case "whites" -> basic.whites();
                case "blacks" -> basic.blacks();
                case "temperature" -> basic.temperature();
                case "tint" -> basic.tint();
                case "texture" -> basic.texture();
                case "clarity" -> basic.clarity();
                case "dehaze" -> basic.dehaze();
                case "vibrance" -> basic.vibrance();
                case "saturation" -> basic.saturation();
                case "orangeSaturation" -> hsl.orangeSaturation();
                case "orangeLuminance" -> hsl.orangeLuminance();
                case "greenSaturation" -> hsl.greenSaturation();
                case "greenLuminance" -> hsl.greenLuminance();
                case "blueSaturation" -> hsl.blueSaturation();
                case "blueLuminance" -> hsl.blueLuminance();
                case "grain" -> effects.grain();
                case "vignette" -> effects.vignette();
                default -> null;
            };
        }

        private static String formatValue(Object value) {
            if (value instanceof Double number) {
                return String.format(Locale.ROOT, "%.2f", number);
            }
            return String.valueOf(value);
        }

        private static String deltaValue(Object before, Object after) {
            if (before instanceof Double beforeNumber && after instanceof Double afterNumber) {
                return String.format(Locale.ROOT, "%+.2f", afterNumber - beforeNumber);
            }
            if (before instanceof Integer beforeNumber && after instanceof Integer afterNumber) {
                return String.format(Locale.ROOT, "%+d", afterNumber - beforeNumber);
            }
            return "";
        }
    }

    private static class MutableAdjustment {
        private double exposure;
        private int contrast;
        private int highlights;
        private int shadows;
        private int whites;
        private int blacks;
        private int temperature;
        private int tint;
        private int texture;
        private int clarity;
        private int dehaze;
        private int vibrance;
        private int saturation;
        private int redHue;
        private int redSaturation;
        private int redLuminance;
        private int orangeHue;
        private int orangeSaturation;
        private int orangeLuminance;
        private int yellowHue;
        private int yellowSaturation;
        private int yellowLuminance;
        private int greenHue;
        private int greenSaturation;
        private int greenLuminance;
        private int aquaHue;
        private int aquaSaturation;
        private int aquaLuminance;
        private int blueHue;
        private int blueSaturation;
        private int blueLuminance;
        private int purpleHue;
        private int purpleSaturation;
        private int purpleLuminance;
        private int magentaHue;
        private int magentaSaturation;
        private int magentaLuminance;
        private int grain;
        private int vignette;

        private static MutableAdjustment from(ColorAdjustment source) {
            MutableAdjustment value = new MutableAdjustment();
            LightroomBasicParams basic = source.basic() == null ? defaultBasic() : source.basic();
            LightroomHslParams hsl = source.hsl() == null ? defaultHsl() : source.hsl();
            LightroomEffectsParams effects = source.effects() == null ? defaultEffects() : source.effects();
            value.exposure = basic.exposure();
            value.contrast = basic.contrast();
            value.highlights = basic.highlights();
            value.shadows = basic.shadows();
            value.whites = basic.whites();
            value.blacks = basic.blacks();
            value.temperature = basic.temperature();
            value.tint = basic.tint();
            value.texture = basic.texture();
            value.clarity = basic.clarity();
            value.dehaze = basic.dehaze();
            value.vibrance = basic.vibrance();
            value.saturation = basic.saturation();
            value.redHue = hsl.redHue();
            value.redSaturation = hsl.redSaturation();
            value.redLuminance = hsl.redLuminance();
            value.orangeHue = hsl.orangeHue();
            value.orangeSaturation = hsl.orangeSaturation();
            value.orangeLuminance = hsl.orangeLuminance();
            value.yellowHue = hsl.yellowHue();
            value.yellowSaturation = hsl.yellowSaturation();
            value.yellowLuminance = hsl.yellowLuminance();
            value.greenHue = hsl.greenHue();
            value.greenSaturation = hsl.greenSaturation();
            value.greenLuminance = hsl.greenLuminance();
            value.aquaHue = hsl.aquaHue();
            value.aquaSaturation = hsl.aquaSaturation();
            value.aquaLuminance = hsl.aquaLuminance();
            value.blueHue = hsl.blueHue();
            value.blueSaturation = hsl.blueSaturation();
            value.blueLuminance = hsl.blueLuminance();
            value.purpleHue = hsl.purpleHue();
            value.purpleSaturation = hsl.purpleSaturation();
            value.purpleLuminance = hsl.purpleLuminance();
            value.magentaHue = hsl.magentaHue();
            value.magentaSaturation = hsl.magentaSaturation();
            value.magentaLuminance = hsl.magentaLuminance();
            value.grain = effects.grain();
            value.vignette = effects.vignette();
            return value;
        }

        private static LightroomBasicParams defaultBasic() {
            return new LightroomBasicParams(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        private static LightroomHslParams defaultHsl() {
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

        private static LightroomEffectsParams defaultEffects() {
            return new LightroomEffectsParams(0, 0);
        }

        private ColorAdjustment toAdjustment(ColorAdjustment source) {
            return new ColorAdjustment(
                    null,
                    source.photoId(),
                    source.style(),
                    source.reason(),
                    new LightroomBasicParams(exposure, contrast, highlights, shadows, whites, blacks, temperature, tint, texture, clarity, dehaze, vibrance, saturation),
                    new LightroomHslParams(
                            redHue, redSaturation, redLuminance,
                            orangeHue, orangeSaturation, orangeLuminance,
                            yellowHue, yellowSaturation, yellowLuminance,
                            greenHue, greenSaturation, greenLuminance,
                            aquaHue, aquaSaturation, aquaLuminance,
                            blueHue, blueSaturation, blueLuminance,
                            purpleHue, purpleSaturation, purpleLuminance,
                            magentaHue, magentaSaturation, magentaLuminance
                    ),
                    new LightroomEffectsParams(grain, vignette),
                    source.steps(),
                    source.rawResponse(),
                    Instant.now()
            );
        }
    }
}
