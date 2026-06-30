package com.tonepilot.runtime.observability;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class TraceContextManager {

    public TraceScope open(String sessionId) {
        Map<String, String> previous = ThreadContext.getImmutableContext();
        ThreadContext.clearMap();
        ThreadContext.putAll(previous);
        ThreadContext.put("traceId", UUID.randomUUID().toString());
        putIfPresent("sessionId", sessionId);
        return new TraceScope(previous);
    }

    public String ensureTrace(String sessionId, String step) {
        if (isBlank(ThreadContext.get("traceId"))) {
            ThreadContext.put("traceId", UUID.randomUUID().toString());
        }
        putIfPresent("sessionId", sessionId);
        putIfPresent("step", step);
        return ThreadContext.get("traceId");
    }

    public String currentTraceId() {
        return ThreadContext.get("traceId");
    }

    private void putIfPresent(String key, String value) {
        if (!isBlank(value) && !"null".equals(value)) {
            ThreadContext.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class TraceScope implements AutoCloseable {

        private final Map<String, String> previous;

        private TraceScope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            ThreadContext.clearMap();
            ThreadContext.putAll(previous);
        }
    }
}
