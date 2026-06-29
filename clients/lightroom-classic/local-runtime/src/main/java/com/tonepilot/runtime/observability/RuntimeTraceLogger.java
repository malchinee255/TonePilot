package com.tonepilot.runtime.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.config.RuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RuntimeTraceLogger {

    @Autowired
    private RuntimeProperties properties;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    public void info(String step, String sessionId, Map<String, Object> details) {
        write("INFO", step, sessionId, details);
    }

    public void warn(String step, String sessionId, Map<String, Object> details) {
        write("WARN", step, sessionId, details);
    }

    public void error(String step, String sessionId, Map<String, Object> details) {
        write("ERROR", step, sessionId, details);
    }

    private void write(String level, String step, String sessionId, Map<String, Object> details) {
        try {
            var logDir = java.nio.file.Path.of(properties.getBridge().getRoot(), "logs");
            Files.createDirectories(logDir);
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("timestamp", Instant.now().toString());
            event.put("level", level);
            event.put("step", step);
            event.put("sessionId", sessionId == null ? "" : sessionId);
            event.put("details", details == null ? Map.of() : details);
            Files.writeString(
                    logDir.resolve("local-runtime.log"),
                    objectMapper.writeValueAsString(event) + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
            // 日志不能影响修图主流程。
        }
    }
}
