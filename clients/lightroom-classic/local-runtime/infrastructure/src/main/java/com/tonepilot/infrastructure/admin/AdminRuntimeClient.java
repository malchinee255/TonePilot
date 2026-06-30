package com.tonepilot.infrastructure.admin;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;





import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tonepilot.infrastructure.config.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AdminRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(AdminRuntimeClient.class);

    @Autowired
    private RuntimeProperties properties = new RuntimeProperties();

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public AdminRuntimeClient() {
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

    public List<Map<String, Object>> retrieveKnowledge(String query, int topK) {
        if (properties.getAdmin().getBaseUrl() == null || properties.getAdmin().getBaseUrl().isBlank()) {
            return List.of();
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "query", query == null ? "" : query,
                    "topK", topK <= 0 ? 5 : topK
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getAdmin().getBaseUrl()) + "/api/rag/retrieve"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            var root = objectMapper.readTree(response.body());
            if (!root.path("success").asBoolean(false) || !root.has("data")) {
                return List.of();
            }
            return objectMapper.convertValue(root.path("data"), new TypeReference<>() {
            });
        } catch (Exception exception) {
            log.debug("检索管理端知识库失败，本地 Agent 将继续执行：{}", exception.getMessage());
            return List.of();
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
