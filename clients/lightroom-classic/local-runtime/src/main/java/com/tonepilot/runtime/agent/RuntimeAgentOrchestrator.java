package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RuntimeAgentOrchestrator {

    @Autowired
    private LightroomStateService stateService;

    @Autowired
    private LightroomToolService toolService;

    @Autowired
    private RuleBasedRuntimeAgent ruleAgent;

    @Autowired
    private ModelRuntimeAgent modelAgent;

    @Autowired
    private RuntimeConfigService configService;

    @Autowired
    private AdminRuntimeClient adminRuntimeClient;

    @Autowired
    private RuntimeTraceLogger traceLogger;

    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(Map<String, Object> payload) {
        String sessionId = String.valueOf(payload.getOrDefault("sessionId", "local-session-" + System.currentTimeMillis()));
        String message = String.valueOf(payload.getOrDefault("message", "")).trim();
        String requestedProvider = String.valueOf(payload.getOrDefault("provider", ""));
        traceLogger.info("agent.request.received", sessionId, Map.of(
                "messageLength", message.length(),
                "requestedProvider", requestedProvider
        ));

        if (message.isBlank()) {
            traceLogger.warn("agent.request.rejected", sessionId, Map.of("reason", "empty_message"));
            return Map.of("success", false, "message", "请输入调色或分析指令。");
        }

        Map<String, Object> status = stateService.status();
        if (!Boolean.TRUE.equals(status.get("available"))) {
            traceLogger.warn("agent.lightroom.unavailable", sessionId, status);
            return Map.of(
                    "success", false,
                    "message", "Lightroom 插件不可用：" + status.getOrDefault("message", "请确认 Lightroom 插件正在运行并写入心跳。")
            );
        }
        traceLogger.info("agent.lightroom.available", sessionId, status);

        Map<String, Object> selected = stateService.selectedPhoto();
        if (!Boolean.TRUE.equals(selected.get("available"))) {
            traceLogger.warn("agent.photo.unavailable", sessionId, selected);
            return Map.of("success", false, "message", selected.getOrDefault("message", "Lightroom 当前没有选中照片。"));
        }
        traceLogger.info("agent.photo.loaded", sessionId, Map.of(
                "photo", selected.getOrDefault("photo", Map.of()),
                "hasPreview", selected.containsKey("previewUrl")
        ));

        Map<String, Object> currentAdjustment = selected.get("currentAdjustment") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Map<String, Object> runtimeConfig = configService.readInternalConfig();
        String provider = String.valueOf(payload.getOrDefault("provider", runtimeConfig.getOrDefault("provider", "rule")));
        traceLogger.info("agent.provider.selected", sessionId, Map.of("provider", provider));

        AgentInput agentInput = new AgentInput(message, currentAdjustment);
        AgentTuneResult tuneResult = modelAgent.plan(
                agentInput,
                provider,
                runtimeConfig,
                () -> ruleAgent.plan(agentInput)
        );
        traceLogger.info("agent.plan.finished", sessionId, Map.of(
                "deltaCount", tuneResult.deltas().size(),
                "settingCount", tuneResult.developSettings().size(),
                "analysis", tuneResult.analysis()
        ));

        Map<String, Object> applyResult = toolService.applyDevelopSettings(tuneResult.developSettings());
        traceLogger.info("agent.lightroom.apply.finished", sessionId, applyResult);

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
        traceLogger.info("agent.response.ready", sessionId, Map.of(
                "success", applyResult.getOrDefault("success", true),
                "message", applyResult.getOrDefault("message", tuneResult.assistantMessage())
        ));
        return Map.of(
                "success", applyResult.getOrDefault("success", true),
                "message", applyResult.getOrDefault("message", tuneResult.assistantMessage()),
                "data", data
        );
    }
}
