package com.tonepilot.runtime.admin;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class AdminRuntimeClientTest {

    @Test
    void adminSyncFailureDoesNotBreakLocalEditing() {
        AdminRuntimeClient client = AdminRuntimeClient.disabled();

        assertThatCode(() -> client.recordEvent("session.message", "session-1", Map.of("message", "test")))
                .doesNotThrowAnyException();
    }
}
