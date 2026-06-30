package com.tonepilot.infrastructure.observability;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;





import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RuntimeTraceLogger {

    private static final Logger TRACE_LOG = LogManager.getLogger("tonepilot.trace");
    private static final ZoneId LOG_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private TraceContextManager traceContextManager;

    public void info(String step, String sessionId, Map<String, Object> details) {
        write(Level.INFO, step, sessionId, details);
    }

    public void warn(String step, String sessionId, Map<String, Object> details) {
        write(Level.WARN, step, sessionId, details);
    }

    public void error(String step, String sessionId, Map<String, Object> details) {
        write(Level.ERROR, step, sessionId, details);
    }

    private void write(Level level, String step, String sessionId, Map<String, Object> details) {
        Map<String, Object> event = buildEvent(level.name(), step, sessionId, details);
        TRACE_LOG.log(level, new ObjectMessage(event));
    }

    public Map<String, Object> buildEvent(String level, String step, String sessionId, Map<String, Object> details) {
        String traceId = traceContextManager.ensureTrace(sessionId, step);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ThreadContext.put("spanId", spanId);
        ZonedDateTime now = ZonedDateTime.now(LOG_ZONE);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        event.put("timestampEpochMillis", now.toInstant().toEpochMilli());
        event.put("level", level);
        event.put("traceId", traceId);
        event.put("spanId", spanId);
        event.put("sessionId", normalize(sessionId));
        event.put("step", normalize(step));
        event.put("details", details == null ? Map.of() : details);
        return event;
    }

    private String normalize(String value) {
        return value == null || "null".equals(value) ? "" : value;
    }
}
