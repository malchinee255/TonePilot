package com.tonepilot.domain.observability;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;







import java.util.function.Supplier;

public final class TraceContext {

    private static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static <T> T useRunId(String runId, Supplier<T> supplier) {
        String previous = RUN_ID.get();
        if (runId != null && !runId.isBlank()) {
            RUN_ID.set(runId);
        }
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                RUN_ID.remove();
            } else {
                RUN_ID.set(previous);
            }
        }
    }

    public static String currentRunId() {
        return RUN_ID.get();
    }
}
