package com.tonepilot.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.ai")
public class AiProperties {

    private String provider = "rule";
    private boolean fallbackEnabled = true;
    private double temperature = 0.2;
    private boolean responseFormatEnabled = false;
    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig qwen2 = new ProviderConfig();

    public String activeProvider() {
        String override = AiProviderContext.current();
        return normalize(override == null || override.isBlank() ? provider : override);
    }

    public ProviderConfig activeConfig() {
        return switch (activeProvider()) {
            case "openai" -> openai;
            case "qwen2" -> qwen2;
            case "rule" -> null;
            default -> throw new IllegalArgumentException("不支持的大模型供应商：" + activeProvider());
        };
    }

    public boolean modelEnabled() {
        return !"rule".equals(activeProvider());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "rule";
        }
        String normalized = value.trim().toLowerCase();
        if ("qwen".equals(normalized) || "ali".equals(normalized) || "aliyun".equals(normalized)) {
            return "qwen2";
        }
        return normalized;
    }

    @Getter
    @Setter
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String chatModel;
        private String visionModel;
    }
}
