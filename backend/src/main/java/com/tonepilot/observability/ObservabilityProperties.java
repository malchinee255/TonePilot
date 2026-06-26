package com.tonepilot.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tonepilot.observability")
public class ObservabilityProperties {

    private int localBufferSize = 500;

    public int getLocalBufferSize() {
        return localBufferSize;
    }

    public void setLocalBufferSize(int localBufferSize) {
        this.localBufferSize = localBufferSize;
    }
}
