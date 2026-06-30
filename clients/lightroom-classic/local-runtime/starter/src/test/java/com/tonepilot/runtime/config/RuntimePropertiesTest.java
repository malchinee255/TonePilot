package com.tonepilot.starter.config;

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

import static org.assertj.core.api.Assertions.assertThat;

class RuntimePropertiesTest {

    @Test
    void defaultPortAndBridgeRootAreLightroomCompatible() {
        RuntimeProperties properties = new RuntimeProperties();

        assertThat(properties.getBridge().getPort()).isEqualTo(33335);
        assertThat(properties.getBridge().getRoot()).contains(".tonepilot-lightroom-bridge");
    }
}
