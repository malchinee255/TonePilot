package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RuntimeAgentOrchestrator {

    private final LightroomStateService stateService;
    private final LightroomToolService toolService;
    private final RuleBasedRuntimeAgent ruleAgent;
    private final AdminRuntimeClient adminRuntimeClient;

    @Autowired
    public RuntimeAgentOrchestrator(
            LightroomStateService stateService,
            LightroomToolService toolService,
            RuleBasedRuntimeAgent ruleAgent,
            AdminRuntimeClient adminRuntimeClient
    ) {
        this.stateService = stateService;
        this.toolService = toolService;
        this.ruleAgent = ruleAgent;
        this.adminRuntimeClient = adminRuntimeClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(Map<String, Object> payload) {
        String message = String.valueOf(payload.getOrDefault("message", "")).trim();
        if (message.isBlank()) {
            return Map.of("success", false, "message", "请输入调色或分析指令。");
        }
        Map<String, Object> selected = stateService.selectedPhoto();
        if (!Boolean.TRUE.equals(selected.get("available"))) {
            return Map.of("success", false, "message", selected.getOrDefault("message", "Lightroom 当前没有选中照片。"));
        }
        Map<String, Object> currentAdjustment = selected.get("currentAdjustment") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        AgentTuneResult tuneResult = ruleAgent.plan(new AgentInput(message, currentAdjustment));
        Map<String, Object> applyResult = toolService.applyDevelopSettings(tuneResult.developSettings());
        String sessionId = String.valueOf(payload.getOrDefault("sessionId", "local-session-" + System.currentTimeMillis()));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("photo", selected.getOrDefault("photo", Map.of()));
        data.put("analysis", tuneResult.analysis());
        data.put("assistantMessage", tuneResult.assistantMessage());
        data.put("deltas", tuneResult.deltas());
        data.put("developSettings", tuneResult.developSettings());
        data.put("apply", applyResult);
        data.put("beforePreviewUrl", selected.getOrDefault("baselinePreviewUrl", selected.get("previewUrl")));
        data.put("afterPreviewUrl", applyResult.getOrDefault("previewUrl", ""));

        adminRuntimeClient.recordEvent("agent.message.finished", sessionId, data);
        return Map.of(
                "success", applyResult.getOrDefault("success", true),
                "message", applyResult.getOrDefault("message", tuneResult.assistantMessage()),
                "data", data
        );
    }
}
