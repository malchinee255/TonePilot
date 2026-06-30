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
