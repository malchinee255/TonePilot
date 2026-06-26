package com.tonepilot.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tonepilot.ai")
public class AiProperties {

    private String provider = "rule";
    private boolean fallbackEnabled = true;
    private double temperature = 0.2;
    private boolean responseFormatEnabled = false;
    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig qwen2 = new ProviderConfig();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isResponseFormatEnabled() {
        return responseFormatEnabled;
    }

    public void setResponseFormatEnabled(boolean responseFormatEnabled) {
        this.responseFormatEnabled = responseFormatEnabled;
    }

    public ProviderConfig getOpenai() {
        return openai;
    }

    public void setOpenai(ProviderConfig openai) {
        this.openai = openai;
    }

    public ProviderConfig getQwen2() {
        return qwen2;
    }

    public void setQwen2(ProviderConfig qwen2) {
        this.qwen2 = qwen2;
    }

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

    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String chatModel;
        private String visionModel;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getVisionModel() {
            return visionModel;
        }

        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }
    }
}
