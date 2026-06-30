package com.tonepilot.starter.admin;

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





import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AdminRuntimeClientTest {

    @Test
    void adminSyncFailureDoesNotBreakLocalEditing() {
        AdminRuntimeClient client = new AdminRuntimeClient();

        assertThatCode(() -> client.recordEvent("session.message", "session-1", Map.of("message", "test")))
                .doesNotThrowAnyException();
    }
    @Test
    void registersDeviceBeforeSendingRuntimeEvent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> received = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/runtime/devices/register", exchange -> {
            received.add(mapper.readValue(exchange.getRequestBody(), Map.class));
            byte[] body = "{\"success\":true,\"data\":{\"userId\":\"usr-test\",\"deviceId\":\"dev-test\",\"created\":true}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/runtime/events", exchange -> {
            received.add(mapper.readValue(exchange.getRequestBody(), Map.class));
            byte[] body = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            RuntimeProperties properties = new RuntimeProperties();
            properties.getAdmin().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getAdmin().setDeviceToken("device-token-test");

            AdminRuntimeClient client = new AdminRuntimeClient();
            ReflectionTestUtils.setField(client, "properties", properties);
            ReflectionTestUtils.setField(client, "objectMapper", mapper);

            client.recordEvent("llm.response", "session-1", Map.of("traceId", "trace-1", "answer", "模型回答"));

            assertThat(received).hasSize(2);
            assertThat(received.get(0)).containsEntry("fingerprint", "device-token-test");
            assertThat(received.get(1)).containsEntry("userId", "usr-test");
            assertThat(received.get(1)).containsEntry("deviceId", "dev-test");
            assertThat(received.get(1)).containsEntry("eventType", "llm.response");
            assertThat(received.get(1)).containsEntry("sessionId", "session-1");
        } finally {
            server.stop(0);
        }
    }

}
