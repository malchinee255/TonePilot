package com.tonepilot.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.security")
public class SecurityProperties {

    private boolean apiKeyEnabled = false;
    private String apiKey = "";
}
