package com.tonepilot.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int requestsPerMinute = 120;
}
