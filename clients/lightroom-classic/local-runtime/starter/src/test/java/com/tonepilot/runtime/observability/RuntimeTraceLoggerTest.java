package com.tonepilot.starter.observability;

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





import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RuntimeTraceLoggerTest {

    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    @Test
    void buildsStructuredTraceEventWithShanghaiTimestampAndTraceFields() {
        RuntimeTraceLogger logger = new RuntimeTraceLogger();
        ReflectionTestUtils.setField(logger, "traceContextManager", new TraceContextManager());

        Map<String, Object> event = logger.buildEvent("INFO", "agent.request.received", "session-1", Map.of("provider", "qwen2"));

        assertThat(event).containsEntry("level", "INFO");
        assertThat(event).containsEntry("step", "agent.request.received");
        assertThat(event).containsEntry("sessionId", "session-1");
        assertThat(event).containsKey("traceId");
        assertThat(event).containsKey("spanId");
        assertThat(event).containsKey("timestampEpochMillis");
        assertThat(String.valueOf(event.get("timestamp"))).contains("+08:00");
        assertThat(OffsetDateTime.parse(String.valueOf(event.get("timestamp")))).isNotNull();
        assertThat((Map<String, Object>) event.get("details")).containsEntry("provider", "qwen2");
    }

    @Test
    void forwardsStructuredTraceEventToAdminRuntimeClient() {
        RuntimeTraceLogger logger = new RuntimeTraceLogger();
        TraceContextManager traceContextManager = new TraceContextManager();
        AdminRuntimeClient adminRuntimeClient = mock(AdminRuntimeClient.class);
        ReflectionTestUtils.setField(logger, "traceContextManager", traceContextManager);
        ReflectionTestUtils.setField(logger, "adminRuntimeClient", adminRuntimeClient);

        logger.info("model.response.received", "session-1", Map.of("answer", "模型回答"));

        verify(adminRuntimeClient).recordEvent(eq("model.response.received"), eq("session-1"), org.mockito.ArgumentMatchers.argThat(event ->
                event.containsKey("traceId")
                        && "model.response.received".equals(event.get("step"))
                        && ((Map<?, ?>) event.get("details")).containsValue("模型回答")
        ));
    }

    @Test
    void log4jConfigurationUsesJsonRollingFileForTraceLogs() throws Exception {
        String xml;
        try (var input = getClass().getResourceAsStream("/log4j2-spring.xml")) {
            assertThat(input).isNotNull();
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(xml).contains("RollingFile");
        assertThat(xml).contains("JsonLayout");
        assertThat(xml).contains("tonepilot.trace");
        assertThat(xml).contains("local-runtime.log");
    }
}
