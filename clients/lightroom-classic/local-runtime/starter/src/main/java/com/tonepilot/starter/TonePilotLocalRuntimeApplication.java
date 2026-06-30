package com.tonepilot.starter;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.config.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.lightroom.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.starter.*;





import com.tonepilot.infrastructure.config.RuntimeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Path;

@SpringBootApplication(scanBasePackages = "com.tonepilot")
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
