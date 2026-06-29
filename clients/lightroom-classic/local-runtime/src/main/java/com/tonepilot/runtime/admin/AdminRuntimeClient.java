package com.tonepilot.runtime.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.config.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class AdminRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(AdminRuntimeClient.class);

    private final RuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AdminRuntimeClient(RuntimeProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }

    public AdminRuntimeClient(RuntimeProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public static AdminRuntimeClient disabled() {
        return new AdminRuntimeClient(new RuntimeProperties(), new ObjectMapper(), HttpClient.newHttpClient());
    }

    public void recordEvent(String eventType, String sessionId, Map<String, Object> payload) {
        if (properties.getAdmin().getBaseUrl() == null || properties.getAdmin().getBaseUrl().isBlank()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", "local-unbound-user",
                    "deviceId", deviceId(),
                    "eventType", eventType,
                    "sessionId", sessionId == null ? "" : sessionId,
                    "payload", payload == null ? Map.of() : payload
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getAdmin().getBaseUrl()) + "/api/runtime/events"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception exception) {
            log.debug("上报管理端运行时事件失败，本地修图流程继续执行：{}", exception.getMessage());
        }
    }

    private String deviceId() {
        if (properties.getAdmin().getDeviceToken() != null && !properties.getAdmin().getDeviceToken().isBlank()) {
            return properties.getAdmin().getDeviceToken();
        }
        return "local-runtime";
    }

    private String trimSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}
