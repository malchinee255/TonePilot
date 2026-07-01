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
import java.util.List;
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

    @Test
    void writesLocalAdjustmentPlanIntoApplyJobWithoutGlobalSettings() throws Exception {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getBridge().setRoot(tempDir.toString());
        properties.getBridge().setLightroomRoot(tempDir.toString());

        FileLightroomToolRepository repository = new FileLightroomToolRepository();
        ReflectionTestUtils.setField(repository, "properties", properties);
        ReflectionTestUtils.setField(repository, "traceLogger", mock(RuntimeTraceLogger.class));

        Map<String, Object> skyMask = Map.of(
                "type", "linear_gradient",
                "target", "天空",
                "coordinateSpace", "normalized_crop",
                "region", Map.of("x", 0.0, "y", 0.0, "w", 1.0, "h", 0.42),
                "settings", Map.of("Exposure2012", -0.25, "Highlights2012", -18)
        );

        repository.applyAdjustments(Map.of(), List.of(skyMask));

        Path job = Files.list(tempDir.resolve("apply-jobs"))
                .filter(path -> path.getFileName().toString().endsWith(".lua"))
                .findFirst()
                .orElseThrow();
        String content = Files.readString(job);

        assertThat(content).contains("[\"localAdjustments\"]=");
        assertThat(content).contains("[\"type\"]=\"linear_gradient\"");
        assertThat(content).contains("[\"target\"]=\"天空\"");
        assertThat(content).contains("[\"Exposure2012\"]=-0.25");
        assertThat(content).contains("[\"Highlights2012\"]=-18");
    }

}
