package com.tonepilot.runtime.bridge;

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
