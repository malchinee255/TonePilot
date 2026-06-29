package com.tonepilot.runtime;

import com.tonepilot.runtime.config.RuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RuntimeProperties.class)
public class TonePilotLocalRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TonePilotLocalRuntimeApplication.class, args);
    }
}
