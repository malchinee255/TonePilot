package com.tonepilot.runtime.config;

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

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePropertiesTest {

    @Test
    void defaultPortAndBridgeRootAreLightroomCompatible() {
        RuntimeProperties properties = new RuntimeProperties();

        assertThat(properties.getBridge().getPort()).isEqualTo(33335);
        assertThat(properties.getBridge().getRoot()).contains(".tonepilot-lightroom-bridge");
    }
}
