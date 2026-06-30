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
