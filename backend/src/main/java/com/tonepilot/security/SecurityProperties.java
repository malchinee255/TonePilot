package com.tonepilot.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tonepilot.security")
public class SecurityProperties {

    private boolean apiKeyEnabled = false;
    private String apiKey = "";

    public boolean isApiKeyEnabled() {
        return apiKeyEnabled;
    }

    public void setApiKeyEnabled(boolean apiKeyEnabled) {
        this.apiKeyEnabled = apiKeyEnabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
