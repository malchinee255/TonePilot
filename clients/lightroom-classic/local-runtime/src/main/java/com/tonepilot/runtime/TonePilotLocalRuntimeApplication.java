package com.tonepilot.runtime;

import com.tonepilot.runtime.config.RuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Path;

@SpringBootApplication
@EnableConfigurationProperties(RuntimeProperties.class)
public class TonePilotLocalRuntimeApplication {

    public static void main(String[] args) {
        configureLogRoot();
        SpringApplication.run(TonePilotLocalRuntimeApplication.class, args);
    }

    private static void configureLogRoot() {
        if (System.getProperty("tonepilot.log.root") != null) {
            return;
        }
        String bridgeRoot = System.getenv().getOrDefault(
                "TONEPILOT_LIGHTROOM_BRIDGE_ROOT",
                Path.of(System.getProperty("user.home"), ".tonepilot-lightroom-bridge").toString()
        );
        System.setProperty("tonepilot.log.root", Path.of(bridgeRoot, "logs").toString());
    }
}
