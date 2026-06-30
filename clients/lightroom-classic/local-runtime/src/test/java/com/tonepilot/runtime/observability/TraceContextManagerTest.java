package com.tonepilot.runtime.observability;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;


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
