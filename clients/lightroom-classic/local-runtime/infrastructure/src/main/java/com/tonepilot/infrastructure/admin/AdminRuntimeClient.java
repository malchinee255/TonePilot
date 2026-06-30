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
import java.util.LinkedHashMap;
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

    private volatile RuntimeIdentity runtimeIdentity;

    public AdminRuntimeClient() {
    }

    public void recordEvent(String eventType, String sessionId, Map<String, Object> payload) {
        if (!adminEnabled()) {
            return;
        }
        try {
            RuntimeIdentity identity = resolveIdentity();
            String body = objectMapper.writeValueAsString(Map.of(
                    "userId", identity.userId(),
                    "deviceId", identity.deviceId(),
                    "eventType", eventType == null ? "" : eventType,
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

    private RuntimeIdentity resolveIdentity() {
        RuntimeIdentity cached = runtimeIdentity;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (runtimeIdentity != null) {
                return runtimeIdentity;
            }
            RuntimeIdentity registered = registerDevice();
            if (registered.registered()) {
                runtimeIdentity = registered;
            }
            return registered;
        }
    }

    private RuntimeIdentity registerDevice() {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("runtime", "lightroom-classic-local");
            metadata.put("bridgeHost", properties.getBridge().getHost());
            metadata.put("bridgePort", properties.getBridge().getPort());
            metadata.put("bridgeRoot", properties.getBridge().getRoot());
            String body = objectMapper.writeValueAsString(Map.of(
                    "fingerprint", deviceId(),
                    "deviceName", "TonePilot Local Runtime",
                    "endpoint", "http://" + properties.getBridge().getHost() + ":" + properties.getBridge().getPort(),
                    "metadata", metadata
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getAdmin().getBaseUrl()) + "/api/runtime/devices/register"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackIdentity();
            }
            var root = objectMapper.readTree(response.body());
            var data = root.path("data");
            String userId = data.path("userId").asText("");
            String registeredDeviceId = data.path("deviceId").asText("");
            if (userId.isBlank() || registeredDeviceId.isBlank()) {
                return fallbackIdentity();
            }
            return new RuntimeIdentity(userId, registeredDeviceId, true);
        } catch (Exception exception) {
            log.debug("注册本地运行时设备失败，本地修图流程继续执行：{}", exception.getMessage());
            return fallbackIdentity();
        }
    }

    private RuntimeIdentity fallbackIdentity() {
        return new RuntimeIdentity("local-unbound-user", deviceId(), false);
    }

    private boolean adminEnabled() {
        return properties.getAdmin().getBaseUrl() != null && !properties.getAdmin().getBaseUrl().isBlank();
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

    private record RuntimeIdentity(String userId, String deviceId, boolean registered) {
    }
}
