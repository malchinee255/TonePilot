package com.tonepilot.runtime;

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
