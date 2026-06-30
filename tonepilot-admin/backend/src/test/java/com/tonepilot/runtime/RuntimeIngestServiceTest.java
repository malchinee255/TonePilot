package com.tonepilot.runtime;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;




import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "tonepilot.persistence.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:runtime-ingest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
class RuntimeIngestServiceTest {

    @Autowired
    private RuntimeIngestService service;

    @Test
    void registersDeviceAndCreatesUserWhenFingerprintIsNew() {
        RuntimeDeviceRegistrationResponse response = service.registerDevice(
                new RuntimeDeviceRegistrationRequest(
                        "fingerprint-1",
                        "TonePilot Local Runtime",
                        "127.0.0.1",
                        Map.of("os", "Windows")
                )
        );

        assertThat(response.userId()).isNotBlank();
        assertThat(response.deviceId()).isNotBlank();
        assertThat(response.created()).isTrue();
    }

    @Test
    void appendsRuntimeEventForRegisteredDevice() {
        RuntimeDeviceRegistrationResponse response = service.registerDevice(
                new RuntimeDeviceRegistrationRequest(
                        "fingerprint-2",
                        "TonePilot Local Runtime",
                        "127.0.0.1",
                        Map.of("os", "Windows")
                )
        );

        service.recordEvent(new RuntimeEventRequest(
                response.userId(),
                response.deviceId(),
                "session.message",
                "session-1",
                Map.of("message", "夜景电影感")
        ));

        assertThat(service.listEvents(response.userId())).hasSize(1);
    }
}
