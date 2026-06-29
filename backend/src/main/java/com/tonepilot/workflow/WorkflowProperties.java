package com.tonepilot.workflow;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.workflow")
public class WorkflowProperties {

    private boolean redisEnabled = true;
    private String redisKeyPrefix = "tonepilot:workflow:";
    private Duration ttl = Duration.ofHours(24);

}
