package com.tonepilot.runtime.observability;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
