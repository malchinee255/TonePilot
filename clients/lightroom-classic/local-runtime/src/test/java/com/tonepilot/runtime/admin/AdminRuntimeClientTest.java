package com.tonepilot.runtime.admin;

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


import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class AdminRuntimeClientTest {

    @Test
    void adminSyncFailureDoesNotBreakLocalEditing() {
        AdminRuntimeClient client = new AdminRuntimeClient();

        assertThatCode(() -> client.recordEvent("session.message", "session-1", Map.of("message", "test")))
                .doesNotThrowAnyException();
    }
}
