package com.tonepilot.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.observability")
public class ObservabilityProperties {

    private int localBufferSize = 500;
}
