package com.tonepilot.starter.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.infrastructure.config.RuntimeProperties;
import com.tonepilot.infrastructure.lightroom.repository.FileLightroomStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileLightroomStateRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void selectedPreviewUrlUsesImmutableSnapshotInsteadOfOverwrittenLiveFile() throws Exception {
        RuntimeProperties properties = new RuntimeProperties();
        properties.getBridge().setRoot(tempDir.toString());
        properties.getBridge().setLightroomRoot(tempDir.toString());
        Files.createDirectories(tempDir.resolve("results"));

        FileLightroomStateRepository repository = new FileLightroomStateRepository();
        ReflectionTestUtils.setField(repository, "properties", properties);
        ReflectionTestUtils.setField(repository, "objectMapper", new ObjectMapper());

        Files.writeString(tempDir.resolve("selected-photo.json"), """
                {"available":true,"updatedAt":100,"photo":{"path":"C:/photo/DSCF1709.RAF","fileName":"DSCF1709.RAF"}}
                """);
        Files.writeString(tempDir.resolve("results").resolve("selected-preview.jpg"), "before-image");

        Map<String, Object> first = repository.selectedPhoto();
        String firstUrl = String.valueOf(first.get("previewUrl"));
        Path firstSnapshot = repository.resultFile(firstUrl.substring("/files/".length(), firstUrl.indexOf("?t=")));

        Files.writeString(tempDir.resolve("selected-photo.json"), """
                {"available":true,"updatedAt":120,"photo":{"path":"C:/photo/DSCF1709.RAF","fileName":"DSCF1709.RAF"}}
                """);
        Files.writeString(tempDir.resolve("results").resolve("selected-preview.jpg"), "after-image");

        Map<String, Object> second = repository.selectedPhoto();
        String secondUrl = String.valueOf(second.get("previewUrl"));

        assertThat(firstUrl).isNotEqualTo(secondUrl);
        assertThat(Files.readString(firstSnapshot)).isEqualTo("before-image");
        assertThat(secondUrl).contains("selected-preview-");
    }
}
