package com.tonepilot.runtime.bridge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BridgePathsTest {

    @Test
    void keepsWindowsLightroomPathWhenConfigured() {
        BridgePaths paths = new BridgePaths(
                Path.of("/tmp/tonepilot"),
                "C:\\Users\\tester\\.tonepilot-lightroom-bridge"
        );

        assertThat(paths.lightroom("apply-jobs", "job.lua"))
                .isEqualTo("C:\\Users\\tester\\.tonepilot-lightroom-bridge\\apply-jobs\\job.lua");
    }
}
