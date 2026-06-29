package com.tonepilot.runtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Data
@ConfigurationProperties(prefix = "tonepilot.runtime")
public class RuntimeProperties {

    private Bridge bridge = new Bridge();
    private Admin admin = new Admin();

    @Data
    public static class Bridge {
        private int port = 33335;
        private String host = "0.0.0.0";
        private String root = defaultBridgeRoot();
        private String lightroomRoot = root;
        private long applyTimeoutMs = 60000;

        private static String defaultBridgeRoot() {
            return Path.of(System.getProperty("user.home"), ".tonepilot-lightroom-bridge").toString();
        }
    }

    @Data
    public static class Admin {
        private String baseUrl = "";
        private String deviceToken = "";
    }
}
