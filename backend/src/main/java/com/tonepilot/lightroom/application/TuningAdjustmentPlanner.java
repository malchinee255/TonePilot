package com.tonepilot.lightroom.application;

import com.tonepilot.agent.ParamValidationAgent;
import com.tonepilot.agent.ParamValidationResult;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.colorgrading.domain.LightroomBasicParams;
import com.tonepilot.colorgrading.domain.LightroomEffectsParams;
import com.tonepilot.colorgrading.domain.LightroomHslParams;
import com.tonepilot.lightroom.domain.ParameterDelta;
import com.tonepilot.lightroom.domain.TuningPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class TuningAdjustmentPlanner {

    private final ParamValidationAgent paramValidationAgent;

    @Autowired
    public TuningAdjustmentPlanner(ParamValidationAgent paramValidationAgent) {
        this.paramValidationAgent = paramValidationAgent;
    }

    public TuningPlan apply(ColorAdjustment source, String message) {
        String text = normalize(message);
        source = normalizeSource(source);
        MutableAdjustment mutable = MutableAdjustment.from(source);
        List<ParameterDelta> requestedDeltas = new ArrayList<>();

        applyBasicIntent(source, mutable, requestedDeltas, text);
        applyStyleIntent(source, mutable, requestedDeltas, text);
        applyExtendedIntent(source, mutable, requestedDeltas, text);

        if (requestedDeltas.isEmpty()) {
            return new TuningPlan(source, List.of(), "没有识别到明确的调色动作，可以试试“再亮一点”“增加电影感”“降低绿色”这类指令。");
        }

        ColorAdjustment candidate = mutable.toAdjustment(source);
        ParamValidationResult validation = paramValidationAgent.validate(candidate);
        List<ParameterDelta> finalDeltas = AdjustmentDiffer.diff(source, validation.adjustment(), requestedDeltas);
        String assistantMessage = "已根据你的描述微调 " + finalDeltas.size() + " 个参数：" +
                String.join("、", finalDeltas.stream().map(ParameterDelta::label).toList()) + "。";
        return new TuningPlan(validation.adjustment(), finalDeltas, assistantMessage);
    }

    private void applyBasicIntent(ColorAdjustment source, MutableAdjustment mutable, List<ParameterDelta> requestedDeltas, String text) {
        if (containsAny(text, "亮一点", "更亮", "提亮", "亮一亮", "太暗")) {
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
        if (containsAny(text, "暖一点", "更暖", "偏暖", "暖调", "白平衡暖")) {
            mutable.temperature += 6;
            requestedDeltas.add(delta("basic", "temperature", "色温", source.basic().temperature(), mutable.temperature, "+6", "用户明确希望白平衡更暖"));
        }
        if (containsAny(text, "冷一点", "更冷", "偏冷", "冷调", "白平衡冷")) {
            mutable.temperature -= 6;
            requestedDeltas.add(delta("basic", "temperature", "色温", source.basic().temperature(), mutable.temperature, "-6", "用户明确希望白平衡更冷"));
        }
        if (containsAny(text, "偏绿", "去绿", "减少绿色偏色")) {
            mutable.tint += 6;
            requestedDeltas.add(delta("basic", "tint", "色调", source.basic().tint(), mutable.tint, "+6", "减少绿色偏色"));
        }
        if (containsAny(text, "偏洋红", "太紫", "去紫")) {
            mutable.tint -= 6;
            requestedDeltas.add(delta("basic", "tint", "色调", source.basic().tint(), mutable.tint, "-6", "减少洋红偏色"));
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
    }

    private void applyStyleIntent(ColorAdjustment source, MutableAdjustment mutable, List<ParameterDelta> requestedDeltas, String text) {
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
    }

    private void applyExtendedIntent(ColorAdjustment source, MutableAdjustment mutable, List<ParameterDelta> requestedDeltas, String text) {
        if (containsAny(text, "锐化", "更清晰", "细节")) {
            addExtendedDelta(source, mutable, requestedDeltas, "detail", "sharpness", "锐化数量", 45, "增强照片细节锐度");
            addExtendedDelta(source, mutable, requestedDeltas, "detail", "sharpenDetail", "锐化细节", 25, "保留边缘和纹理细节");
        }
        if (containsAny(text, "降噪", "噪点", "噪声")) {
            addExtendedDelta(source, mutable, requestedDeltas, "detail", "luminanceSmoothing", "明亮度降噪", 30, "降低亮度噪声");
            addExtendedDelta(source, mutable, requestedDeltas, "detail", "colorNoiseReduction", "颜色降噪", 25, "降低彩色噪声");
        }
        if (containsAny(text, "曲线", "压高光曲线", "抬暗部曲线")) {
            addExtendedDelta(source, mutable, requestedDeltas, "toneCurve", "parametricHighlights", "曲线高光", -12, "使用参数曲线压高光");
            addExtendedDelta(source, mutable, requestedDeltas, "toneCurve", "parametricShadows", "曲线阴影", 10, "使用参数曲线抬暗部");
            addExtendedDelta(source, mutable, requestedDeltas, "toneCurve", "parametricHighlightSplit", "高光拆分", 70, "调整参数曲线高光范围");
        }
        if (containsAny(text, "颜色分级", "阴影偏色", "高光偏色", "中间调")) {
            addExtendedDelta(source, mutable, requestedDeltas, "colorGrading", "colorGradeShadowHue", "阴影色相", 220, "给阴影加入冷色倾向");
            addExtendedDelta(source, mutable, requestedDeltas, "colorGrading", "colorGradeShadowSat", "阴影饱和度", 8, "控制阴影颜色分级强度");
            addExtendedDelta(source, mutable, requestedDeltas, "colorGrading", "colorGradeHighlightHue", "高光色相", 42, "给高光加入暖色倾向");
            addExtendedDelta(source, mutable, requestedDeltas, "colorGrading", "colorGradeHighlightSat", "高光饱和度", 6, "控制高光颜色分级强度");
            addExtendedDelta(source, mutable, requestedDeltas, "colorGrading", "colorGradeBlending", "颜色分级混合", 50, "平衡阴影和高光分级过渡");
        }
        if (containsAny(text, "镜头校正", "配置文件校正", "畸变校正", "色差")) {
            addExtendedDelta(source, mutable, requestedDeltas, "lensCorrections", "lensProfileEnable", "启用配置文件校正", 1, "启用镜头配置文件校正");
            addExtendedDelta(source, mutable, requestedDeltas, "lensCorrections", "autoLateralCA", "移除色差", 1, "自动移除横向色差");
        }
        if (containsAny(text, "透视", "水平校正", "垂直校正", "建筑校正", "upright")) {
            addExtendedDelta(source, mutable, requestedDeltas, "transform", "uprightTransformMode", "Upright 模式", 2, "自动校正画面透视");
            addExtendedDelta(source, mutable, requestedDeltas, "transform", "perspectiveVertical", "垂直透视", 0, "保持垂直透视可控");
            addExtendedDelta(source, mutable, requestedDeltas, "transform", "perspectiveHorizontal", "水平透视", 0, "保持水平透视可控");
        }
        if (containsAny(text, "校准", "三原色", "红原色", "蓝原色", "绿原色")) {
            addExtendedDelta(source, mutable, requestedDeltas, "calibration", "bluePrimaryHue", "蓝原色色相", -6, "微调蓝原色色相");
            addExtendedDelta(source, mutable, requestedDeltas, "calibration", "bluePrimarySaturation", "蓝原色饱和度", 8, "增强蓝原色表现");
        }
    }

    private void addExtendedDelta(ColorAdjustment source, MutableAdjustment mutable, List<ParameterDelta> requestedDeltas,
                                  String group, String name, String label, Object targetValue, String reason) {
        Object before = source.extended().getOrDefault(name, 0);
        mutable.extended = new LinkedHashMap<>(mutable.extended);
        mutable.extended.put(name, targetValue);
        requestedDeltas.add(new ParameterDelta(group, name, label, String.valueOf(before), String.valueOf(targetValue), deltaValue(before, targetValue), reason));
    }

    private ColorAdjustment normalizeSource(ColorAdjustment source) {
        if (source == null) {
            source = new ColorAdjustment(null, null, "Lightroom 当前参数", "空参数兜底", null, null, null, List.of(), Map.of(), Instant.now());
        }
        return new ColorAdjustment(
                source.id(),
                source.photoId(),
                source.style(),
                source.reason(),
                source.basic() == null ? MutableAdjustment.defaultBasic() : source.basic(),
                source.hsl() == null ? MutableAdjustment.defaultHsl() : source.hsl(),
                source.effects() == null ? MutableAdjustment.defaultEffects() : source.effects(),
                source.extended(),
                source.steps(),
                source.rawResponse(),
                source.createdAt()
        );
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
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

    private String deltaValue(Object before, Object after) {
        if (before instanceof Number beforeNumber && after instanceof Number afterNumber) {
            double diff = afterNumber.doubleValue() - beforeNumber.doubleValue();
            if (Math.rint(diff) == diff) {
                return String.format(Locale.ROOT, "%+d", (int) diff);
            }
            return String.format(Locale.ROOT, "%+.2f", diff);
        }
        return "";
    }

    private static class AdjustmentDiffer {

        private static List<ParameterDelta> diff(ColorAdjustment before, ColorAdjustment after, List<ParameterDelta> requestedDeltas) {
            LinkedHashMap<String, ParameterDelta> result = new LinkedHashMap<>();
            for (ParameterDelta requested : requestedDeltas) {
                Object beforeValue = read(before, requested.name());
                Object afterValue = read(after, requested.name());
                if (afterValue != null && !Objects.equals(beforeValue, afterValue)) {
                    result.put(requested.name(), new ParameterDelta(
                            requested.group(),
                            requested.name(),
                            requested.label(),
                            beforeValue == null ? requested.beforeValue() : formatValue(beforeValue),
                            formatValue(afterValue),
                            beforeValue == null ? requested.delta() : deltaValue(beforeValue, afterValue),
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
                case "redHue" -> hsl.redHue();
                case "redSaturation" -> hsl.redSaturation();
                case "redLuminance" -> hsl.redLuminance();
                case "orangeHue" -> hsl.orangeHue();
                case "orangeSaturation" -> hsl.orangeSaturation();
                case "orangeLuminance" -> hsl.orangeLuminance();
                case "yellowHue" -> hsl.yellowHue();
                case "yellowSaturation" -> hsl.yellowSaturation();
                case "yellowLuminance" -> hsl.yellowLuminance();
                case "greenHue" -> hsl.greenHue();
                case "greenSaturation" -> hsl.greenSaturation();
                case "greenLuminance" -> hsl.greenLuminance();
                case "aquaHue" -> hsl.aquaHue();
                case "aquaSaturation" -> hsl.aquaSaturation();
                case "aquaLuminance" -> hsl.aquaLuminance();
                case "blueHue" -> hsl.blueHue();
                case "blueSaturation" -> hsl.blueSaturation();
                case "blueLuminance" -> hsl.blueLuminance();
                case "purpleHue" -> hsl.purpleHue();
                case "purpleSaturation" -> hsl.purpleSaturation();
                case "purpleLuminance" -> hsl.purpleLuminance();
                case "magentaHue" -> hsl.magentaHue();
                case "magentaSaturation" -> hsl.magentaSaturation();
                case "magentaLuminance" -> hsl.magentaLuminance();
                case "grain" -> effects.grain();
                case "vignette" -> effects.vignette();
                default -> adjustment.extended().get(name);
            };
        }

        private static String formatValue(Object value) {
            if (value instanceof Double number) {
                return String.format(Locale.ROOT, "%.2f", number);
            }
            return String.valueOf(value);
        }

        private static String deltaValue(Object before, Object after) {
            if (before instanceof Number beforeNumber && after instanceof Number afterNumber) {
                double diff = afterNumber.doubleValue() - beforeNumber.doubleValue();
                if (Math.rint(diff) == diff) {
                    return String.format(Locale.ROOT, "%+d", (int) diff);
                }
                return String.format(Locale.ROOT, "%+.2f", diff);
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
        private Map<String, Object> extended = Map.of();

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
            value.extended = source.extended();
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
                    extended,
                    source.steps(),
                    source.rawResponse(),
                    Instant.now()
            );
        }
    }
}
