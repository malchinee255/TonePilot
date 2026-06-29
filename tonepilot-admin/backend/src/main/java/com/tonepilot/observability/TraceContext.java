package com.tonepilot.observability;

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
