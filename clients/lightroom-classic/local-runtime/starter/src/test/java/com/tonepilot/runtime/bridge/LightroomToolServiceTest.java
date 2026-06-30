package com.tonepilot.starter.bridge;

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





import com.tonepilot.infrastructure.observability.RuntimeTraceLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LightroomToolServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesApplyJobAsExecutableLuaChunk() throws Exception {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getBridge().setRoot(tempDir.toString());
        properties.getBridge().setLightroomRoot(tempDir.toString());

        FileLightroomToolRepository repository = new FileLightroomToolRepository();
        ReflectionTestUtils.setField(repository, "properties", properties);
        ReflectionTestUtils.setField(repository, "traceLogger", mock(RuntimeTraceLogger.class));

        repository.applyDevelopSettings(Map.of("Exposure2012", 0.2));

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
