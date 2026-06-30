package com.tonepilot.starter.admin;

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
