package com.tonepilot.runtime.observability;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextManagerTest {

    private final TraceContextManager manager = new TraceContextManager();

    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    @Test
    void opensAndRestoresDistributedTraceContext() {
        ThreadContext.put("traceId", "outer-trace");

        try (TraceContextManager.TraceScope ignored = manager.open("session-1")) {
            assertThat(ThreadContext.get("traceId")).isNotBlank();
            assertThat(ThreadContext.get("traceId")).isNotEqualTo("outer-trace");
            assertThat(ThreadContext.get("sessionId")).isEqualTo("session-1");
        }

        assertThat(ThreadContext.get("traceId")).isEqualTo("outer-trace");
        assertThat(ThreadContext.get("sessionId")).isNull();
    }

    @Test
    void keepsCurrentTraceWhenEnsuringNestedEventContext() {
        try (TraceContextManager.TraceScope ignored = manager.open("session-1")) {
            String traceId = ThreadContext.get("traceId");

            String ensured = manager.ensureTrace("session-1", "agent.request.received");

            assertThat(ensured).isEqualTo(traceId);
            assertThat(ThreadContext.get("step")).isEqualTo("agent.request.received");
        }
    }
}
