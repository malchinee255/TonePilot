package com.tonepilot.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.storage")
public class StorageProperties {

    private String type = "local";
    private String root = "./storage";
    private Minio minio = new Minio();

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "tonepilot";
        private String secretKey = "tonepilot123";
        private String bucket = "tonepilot";
    }
}
