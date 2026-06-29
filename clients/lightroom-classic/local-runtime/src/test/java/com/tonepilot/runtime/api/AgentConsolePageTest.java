package com.tonepilot.runtime.api;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConsolePageTest {

    @Test
    void rendersLeftTabsForPhotoHistoryAndModelSettings() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("data-left-tab=\"photo\"");
        assertThat(html).contains("data-left-tab=\"history\"");
        assertThat(html).contains("data-left-tab=\"model\"");
        assertThat(html).contains("id=\"modelSettingsPanel\"");
        assertThat(html).contains("id=\"versionList\"");
    }
}
