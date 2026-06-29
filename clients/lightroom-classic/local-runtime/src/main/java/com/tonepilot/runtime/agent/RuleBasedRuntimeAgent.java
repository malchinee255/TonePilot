package com.tonepilot.runtime.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedRuntimeAgent {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("Exposure2012", "曝光"),
            Map.entry("Contrast2012", "对比度"),
            Map.entry("Highlights2012", "高光"),
            Map.entry("Shadows2012", "阴影"),
            Map.entry("Whites2012", "白色色阶"),
            Map.entry("Blacks2012", "黑色色阶"),
            Map.entry("Texture", "纹理"),
            Map.entry("Clarity2012", "清晰度"),
            Map.entry("Dehaze", "去朦胧"),
            Map.entry("Vibrance", "自然饱和度"),
            Map.entry("Saturation", "饱和度"),
            Map.entry("Temperature", "色温"),
            Map.entry("Tint", "色调"),
            Map.entry("PostCropVignetteAmount", "暗角"),
            Map.entry("GrainAmount", "颗粒"),
            Map.entry("GreenSaturation", "绿色饱和度"),
            Map.entry("GreenLuminance", "绿色明亮度"),
            Map.entry("BlueSaturation", "蓝色饱和度"),
            Map.entry("BlueLuminance", "蓝色明亮度"),
            Map.entry("OrangeSaturation", "橙色饱和度"),
            Map.entry("OrangeLuminance", "橙色明亮度")
    );

    public AgentTuneResult plan(AgentInput input) {
        String text = input.message() == null ? "" : input.message();
        Map<String, Object> current = input.currentSettings() == null ? Map.of() : input.currentSettings();
        List<Operation> operations = new ArrayList<>();

        if (matches(text, "亮", "提亮", "太暗", "brighter")) {
            operations.add(change("Exposure2012", 0.18));
            operations.add(change("Shadows2012", 14));
        }
        if (matches(text, "暗一点", "压暗", "太亮", "降低亮度")) {
            operations.add(change("Exposure2012", -0.15));
            operations.add(change("Highlights2012", -16));
        }
        if (matches(text, "降低高光", "压高光", "高光")) {
            operations.add(change("Highlights2012", -22));
        }
        if (matches(text, "电影", "cinematic", "夜景", "氛围")) {
            operations.add(change("Highlights2012", -14));
            operations.add(change("Shadows2012", 10));
            operations.add(change("Contrast2012", 18));
            operations.add(change("Dehaze", 5));
            operations.add(change("PostCropVignetteAmount", -8));
        }
        if (matches(text, "柔和", "日系", "干净")) {
            operations.add(change("Contrast2012", -8));
            operations.add(change("Highlights2012", -12));
            operations.add(change("Vibrance", 6));
        }
        if (matches(text, "胶片", "film", "复古")) {
            operations.add(change("Blacks2012", 10));
            operations.add(change("Saturation", -6));
            operations.add(set("GrainAmount", 18));
            operations.add(change("PostCropVignetteAmount", -10));
        }
        if (matches(text, "暖", "更暖", "偏暖", "warm")) {
            operations.add(change("Temperature", 300));
        }
        if (matches(text, "冷", "更冷", "偏冷", "cool")) {
            operations.add(change("Temperature", -300));
        }
        if (matches(text, "绿色", "绿", "脏")) {
            operations.add(change("GreenSaturation", -12));
            operations.add(change("GreenLuminance", 6));
        }
        if (matches(text, "蓝色", "蓝")) {
            operations.add(change("BlueSaturation", -8));
            operations.add(change("BlueLuminance", -6));
        }
        if (matches(text, "肤色", "人像")) {
            operations.add(change("OrangeLuminance", 8));
            operations.add(change("OrangeSaturation", -4));
            operations.add(change("Texture", -6));
        }

        Map<String, Object> developSettings = buildDevelopSettings(current, operations);
        List<AgentDelta> deltas = buildDeltas(current, developSettings);
        return new AgentTuneResult(summarize(text, deltas), developSettings, deltas, analyze(text));
    }

    private Map<String, Object> buildDevelopSettings(Map<String, Object> current, List<Operation> operations) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Operation operation : operations) {
            if (result.containsKey(operation.setting())) {
                continue;
            }
            Object before = currentValue(current, operation.setting());
            Object after = "set".equals(operation.mode())
                    ? operation.value()
                    : clamp(operation.setting(), asDouble(before) + asDouble(operation.delta()));
            if (!String.valueOf(before).equals(String.valueOf(after))) {
                result.put(operation.setting(), after);
            }
        }
        return result;
    }

    private List<AgentDelta> buildDeltas(Map<String, Object> current, Map<String, Object> developSettings) {
        return developSettings.entrySet().stream()
                .map(entry -> {
                    Object before = currentValue(current, entry.getKey());
                    Object after = entry.getValue();
                    Object delta = after instanceof Number && before instanceof Number
                            ? round(asDouble(after) - asDouble(before))
                            : after;
                    return new AgentDelta(
                            groupFor(entry.getKey()),
                            entry.getKey(),
                            LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                            before,
                            after,
                            delta,
                            "根据本轮用户意图生成，未被明确规划的 Lightroom 参数保持不变。"
                    );
                })
                .toList();
    }

    private Map<String, Object> analyze(String text) {
        String photoType = matches(text, "夜景", "灯光", "城市", "city", "night")
                ? "夜景 / 城市氛围照片"
                : "当前 Lightroom 照片";
        String intent = text.isBlank() ? "等待用户输入修图意图" : text;
        return Map.of(
                "intent", intent,
                "photoType", photoType,
                "recommendedStyle", inferStyle(text)
        );
    }

    private String inferStyle(String text) {
        if (matches(text, "电影", "cinematic", "夜景")) {
            return "夜景电影感：压高光、提暗部、增强对比和去朦胧，保留灯光层次";
        }
        if (matches(text, "胶片", "film", "复古")) {
            return "胶片复古：轻微颗粒、抬黑、降低饱和度";
        }
        return "自然微调：只调整用户明确表达的全局参数";
    }

    private String summarize(String message, List<AgentDelta> deltas) {
        if (deltas.isEmpty()) {
            return "我先保留当前参数，没有检测到需要明确修改的全局调色项。";
        }
        String names = deltas.stream().map(AgentDelta::label).reduce((a, b) -> a + "、" + b).orElse("");
        return "已根据「" + message + "」微调 " + deltas.size() + " 个参数：" + names + "。未被明确规划的参数保持不变。";
    }

    private Object currentValue(Map<String, Object> current, String setting) {
        if (current.containsKey(setting)) {
            return current.get(setting);
        }
        Object nested = nestedCurrentValue(current, setting);
        if (nested != null) {
            return nested;
        }
        return setting.startsWith("Enable") ? false : 0.0;
    }

    @SuppressWarnings("unchecked")
    private Object nestedCurrentValue(Map<String, Object> current, String setting) {
        String[] path = switch (setting) {
            case "Exposure2012" -> new String[]{"basic", "exposure"};
            case "Contrast2012" -> new String[]{"basic", "contrast"};
            case "Highlights2012" -> new String[]{"basic", "highlights"};
            case "Shadows2012" -> new String[]{"basic", "shadows"};
            case "Whites2012" -> new String[]{"basic", "whites"};
            case "Blacks2012" -> new String[]{"basic", "blacks"};
            case "Texture" -> new String[]{"basic", "texture"};
            case "Clarity2012" -> new String[]{"basic", "clarity"};
            case "Dehaze" -> new String[]{"basic", "dehaze"};
            case "Vibrance" -> new String[]{"basic", "vibrance"};
            case "Saturation" -> new String[]{"basic", "saturation"};
            case "Temperature" -> new String[]{"basic", "temperature"};
            case "Tint" -> new String[]{"basic", "tint"};
            case "PostCropVignetteAmount" -> new String[]{"effects", "vignette"};
            case "GrainAmount" -> new String[]{"effects", "grain"};
            case "GreenSaturation" -> new String[]{"hsl", "greenSaturation"};
            case "GreenLuminance" -> new String[]{"hsl", "greenLuminance"};
            case "BlueSaturation" -> new String[]{"hsl", "blueSaturation"};
            case "BlueLuminance" -> new String[]{"hsl", "blueLuminance"};
            case "OrangeSaturation" -> new String[]{"hsl", "orangeSaturation"};
            case "OrangeLuminance" -> new String[]{"hsl", "orangeLuminance"};
            default -> null;
        };
        if (path == null || !(current.get(path[0]) instanceof Map<?, ?> group)) {
            return null;
        }
        return ((Map<String, Object>) group).get(path[1]);
    }

    private String groupFor(String setting) {
        if (setting.contains("Saturation") || setting.contains("Luminance") || setting.contains("Hue")) {
            return "hsl";
        }
        if (setting.contains("Grain") || setting.contains("Vignette")) {
            return "effects";
        }
        return "basic";
    }

    private Operation change(String setting, double delta) {
        return new Operation(setting, "change", null, delta);
    }

    private Operation set(String setting, Object value) {
        return new Operation(setting, "set", value, null);
    }

    private boolean matches(String text, String... words) {
        String value = text == null ? "" : text.toLowerCase();
        for (String word : words) {
            if (value.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private double clamp(String setting, double value) {
        double min = setting.equals("Temperature") ? 2000 : -100;
        double max = setting.equals("Temperature") ? 50000 : 100;
        if (setting.equals("Exposure2012")) {
            min = -5;
            max = 5;
        }
        return round(Math.max(min, Math.min(max, value)));
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception exception) {
            return 0.0;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Operation(String setting, String mode, Object value, Object delta) {
    }
}
