package com.tonepilot.ai;

public final class JsonExtractor {

    private JsonExtractor() {
    }

    public static String object(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("模型返回内容为空");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型返回内容不是 JSON 对象：" + content);
        }
        return trimmed.substring(start, end + 1);
    }
}
