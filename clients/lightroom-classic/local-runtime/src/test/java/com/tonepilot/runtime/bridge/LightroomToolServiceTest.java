package com.tonepilot.runtime.bridge;

import com.tonepilot.runtime.config.RuntimeProperties;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LightroomToolServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesApplyJobAsExecutableLuaChunk() throws Exception {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getBridge().setApplyTimeoutMs(1);

        LightroomStateService stateService = mock(LightroomStateService.class);
        when(stateService.bridgePaths()).thenReturn(new BridgePaths(tempDir, tempDir.toString()));

        LightroomToolService service = new LightroomToolService();
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "stateService", stateService);
        ReflectionTestUtils.setField(service, "traceLogger", mock(RuntimeTraceLogger.class));

        service.applyDevelopSettings(Map.of("Exposure2012", 0.2));

        Path job = Files.list(tempDir.resolve("apply-jobs"))
                .filter(path -> path.getFileName().toString().endsWith(".lua"))
                .findFirst()
                .orElseThrow();
        String content = Files.readString(job);

        assertThat(content).startsWith("return {");
        assertThat(content).contains("[\"developSettings\"]=");
        assertThat(content).contains("[\"Exposure2012\"]=0.2");
    }
}
