package com.tonepilot.ai;

import java.util.function.Supplier;

public final class AiProviderContext {

    private static final ThreadLocal<String> OVERRIDE = new ThreadLocal<>();

    private AiProviderContext() {
    }

    public static <T> T use(String provider, Supplier<T> supplier) {
        String previous = OVERRIDE.get();
        if (provider != null && !provider.isBlank()) {
            OVERRIDE.set(provider);
        }
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(previous);
            }
        }
    }

    public static String current() {
        return OVERRIDE.get();
    }
}
