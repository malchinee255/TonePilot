package com.tonepilot.runtime.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.config.RuntimeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeTraceLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesJsonLineToBridgeLogDirectory() throws Exception {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getBridge().setRoot(tempDir.toString());

        RuntimeTraceLogger logger = new RuntimeTraceLogger();
        ReflectionTestUtils.setField(logger, "properties", properties);

        logger.info("agent.request.received", "session-1", Map.of("provider", "qwen2"));

        Path logFile = tempDir.resolve("logs").resolve("local-runtime.log");
        String content = Files.readString(logFile);

        assertThat(content).contains("\"step\":\"agent.request.received\"");
        assertThat(content).contains("\"sessionId\":\"session-1\"");
        assertThat(content).contains("\"provider\":\"qwen2\"");
        assertThat(content).contains("\"timestampEpochMillis\":");
        assertThat(content).contains("+08:00");
        String timestamp = new ObjectMapper().readTree(content).path("timestamp").asText();
        assertThat(OffsetDateTime.parse(timestamp)).isNotNull();
    }
}
