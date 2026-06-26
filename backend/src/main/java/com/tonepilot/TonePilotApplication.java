package com.tonepilot;

import com.tonepilot.ai.AiProperties;
import com.tonepilot.observability.ObservabilityProperties;
import com.tonepilot.persistence.PersistenceProperties;
import com.tonepilot.security.RateLimitProperties;
import com.tonepilot.security.SecurityProperties;
import com.tonepilot.storage.StorageProperties;
import com.tonepilot.workflow.WorkflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AiProperties.class,
        StorageProperties.class,
        WorkflowProperties.class,
        PersistenceProperties.class,
        ObservabilityProperties.class,
        SecurityProperties.class,
        RateLimitProperties.class
})
public class TonePilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TonePilotApplication.class, args);
    }
}
