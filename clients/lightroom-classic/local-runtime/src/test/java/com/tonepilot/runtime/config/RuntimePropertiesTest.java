package com.tonepilot.runtime.config;

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
