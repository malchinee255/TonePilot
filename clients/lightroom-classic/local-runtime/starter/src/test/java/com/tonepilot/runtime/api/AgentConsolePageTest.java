package com.tonepilot.starter.api;

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

        assertThat(html).contains("class=\"left-rail\"");
        assertThat(html).contains("data-left-tab=\"photo\"");
        assertThat(html).contains("data-left-tab=\"history\"");
        assertThat(html).contains("data-left-tab=\"model\"");
        assertThat(html).contains("id=\"modelSettingsPanel\"");
        assertThat(html).contains("id=\"versionList\"");
    }

    @Test
    void clearsPromptImmediatelyAndShowsMainAgentThought() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("prompt.value = ''");
        assertThat(html).contains("addThinkingMessage");
        assertThat(html).contains("思考中");
        assertThat(html).contains("renderAgentThought");
        assertThat(html).contains("/api/lightroom-agent/chat/stream");
        assertThat(html).contains("appendReactEvent");
        assertThat(html).contains("isVisibleReactEvent");
        assertThat(html).contains("return type === 'agent.thought' || type === 'agent.error'");
        assertThat(html).contains("agent.thought");
        assertThat(html).contains("agent.final");
        assertThat(html).doesNotContain("Agent 执行过程");
        assertThat(html).doesNotContain("读取 Lightroom 当前照片和调色上下文");
    }

    @Test
    void replacesThinkingMessageWithFinalAgentResult() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("renderAgentResult(data, thinking)");
        assertThat(html).contains("finalizeAgentMessage");
        assertThat(html).contains("thinking.classList.remove('thinking-message')");
        assertThat(html).doesNotContain("renderAgentResult(data)\n      } catch");
    }


    @Test
    void usesReturnedAfterPreviewUrlWhenApplyJobFinishes() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("result.afterPreviewUrl || result.previewUrl");
        assertThat(html).contains("completeApplyComparison(jobId, afterUrl)");
    }

    @Test
    void locksBeforeAndAfterPreviewToCurrentApplyJob() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("activeApplyJob");
        assertThat(html).contains("lockApplyComparison");
        assertThat(html).contains("completeApplyComparison");
        assertThat(html).contains("isComparisonLocked");
        assertThat(html).contains("fetchFreshSelectedPreviewAfterApply");
    }


    @Test
    void localizesLightroomParameterNamesForUserFacingMessages() throws Exception {
        String html;
        try (var input = getClass().getResourceAsStream("/static/agent-console.html")) {
            assertThat(input).isNotNull();
            html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(html).contains("const PARAM_LABELS");
        assertThat(html).contains("Exposure2012: '曝光'");
        assertThat(html).contains("BlueSaturation: '蓝色饱和度'");
        assertThat(html).contains("localizeParamText");
        assertThat(html).contains("paramDisplayName(delta.label || delta.name)");
        assertThat(html).contains("formatDeltaValue(delta)");
        assertThat(html).contains("filterKnowledgeMatches");
        assertThat(html).contains("本轮调整");
    }

}
