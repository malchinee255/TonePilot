package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuntimeAgentOrchestrator {

    @Autowired
    private LightroomStateService stateService;

    @Autowired
    private LightroomToolService toolService;

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
        String sessionId = sessionId(payload);
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

        Map<String, Object> selected = stateService.selectedPhoto();
        if (!Boolean.TRUE.equals(selected.get("available"))) {
            traceLogger.warn("agent.photo.unavailable", sessionId, selected);
            return Map.of("success", false, "message", selected.getOrDefault("message", "Lightroom 当前没有选中照片。"));
        }

        Map<String, Object> currentAdjustment = selected.get("currentAdjustment") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Map<String, Object> runtimeConfig = configService.readInternalConfig();
        String provider = String.valueOf(payload.getOrDefault("provider", runtimeConfig.getOrDefault("provider", "qwen2")));
        traceLogger.info("agent.provider.selected", sessionId, Map.of("provider", provider));

        AgentInput agentInput = new AgentInput(message, currentAdjustment);
        AgentTuneResult tuneResult;
        try {
            tuneResult = modelAgent.plan(agentInput, provider, runtimeConfig);
        } catch (Exception exception) {
            traceLogger.warn("agent.model.failed", sessionId, Map.of(
                    "provider", provider,
                    "error", exception.getMessage()
            ));
            return Map.of("success", false, "message", exception.getMessage());
        }
        traceLogger.info("agent.intent.analyzed", sessionId, Map.of(
                "deltaCount", tuneResult.deltas().size(),
                "settingCount", tuneResult.developSettings().size(),
                "localAdjustmentCount", tuneResult.localAdjustments().size(),
                "analysis", tuneResult.analysis()
        ));

        Map<String, Object> data = baseData(sessionId, selected, tuneResult);
        data.put("beforePreviewUrl", String.valueOf(selected.getOrDefault("baselinePreviewUrl", selected.getOrDefault("previewUrl", ""))));
        data.put("knowledgeMatches", knowledgeMatches(runtimeConfig));

        if (shouldApply(message, tuneResult)) {
            Map<String, Object> applyResult = toolService.applyDevelopSettings(tuneResult.developSettings());
            data.put("action", "tool_submitted");
            data.put("apply", applyResult);
            data.put("afterPreviewUrl", "");
            data.put("assistantMessage", mergeMessage(
                    tuneResult.assistantMessage(),
                    "我已经根据这个判断调用 Lightroom 执行调色，完成后会刷新修图后预览。"
            ));
            adminRuntimeClient.recordEvent("agent.tool.submitted", sessionId, data);
            traceLogger.info("agent.tool.submitted", sessionId, Map.of("apply", applyResult));
            return Map.of("success", applyResult.getOrDefault("success", true), "message", data.get("assistantMessage"), "data", data);
        }

        data.put("action", "respond");
        data.put("assistantMessage", tuneResult.assistantMessage());
        adminRuntimeClient.recordEvent("agent.message.responded", sessionId, data);
        traceLogger.info("agent.response.ready", sessionId, Map.of("action", "respond"));
        return Map.of("success", true, "message", data.get("assistantMessage"), "data", data);
    }

    public Map<String, Object> applyStatus(String jobId) {
        return toolService.applyStatus(jobId);
    }

    private boolean shouldApply(String message, AgentTuneResult tuneResult) {
        if (tuneResult.developSettings() == null || tuneResult.developSettings().isEmpty()) {
            return false;
        }
        String decision = tuneResult.agentThought() == null ? "" : String.valueOf(tuneResult.agentThought().decision()).trim();
        if (!decision.isBlank()) {
            return "apply_global_adjustments".equals(decision);
        }
        String value = message == null ? "" : message.trim().toLowerCase();
        if (containsAny(value, "不要修", "先别修", "只分析", "分析一下", "看看", "建议", "方案")) {
            return containsAny(value, "修成", "调成", "改成", "执行", "应用", "按这个修");
        }
        return containsAny(value,
                "修", "调", "改", "应用", "执行", "亮", "暗", "冷", "暖", "电影", "胶片",
                "通透", "干净", "鲜艳", "饱和", "对比", "肤色", "绿色", "蓝色", "apply", "edit");
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> baseData(String sessionId, Map<String, Object> selected, AgentTuneResult tuneResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("photo", selected.getOrDefault("photo", Map.of()));
        data.put("analysis", tuneResult.analysis());
        data.put("agentThought", tuneResult.agentThought());
        data.put("assistantMessage", tuneResult.assistantMessage());
        data.put("deltas", tuneResult.deltas());
        data.put("developSettings", tuneResult.developSettings());
        data.put("localAdjustments", tuneResult.localAdjustments());
        data.put("suggestedReplies", tuneResult.agentThought().userOptions() == null ? List.of() : tuneResult.agentThought().userOptions());
        data.put("capabilities", Map.of(
                "supportsGlobalDevelopSettings", true,
                "supportsLocalMasks", false,
                "localMaskMode", "plan_only",
                "message", "当前 Lightroom 插件只会真实执行全局 Develop Settings；局部蒙版先作为 Agent 计划展示。"
        ));
        data.put("modelRawContent", tuneResult.rawModelContent());
        return data;
    }

    private List<Map<String, Object>> knowledgeMatches(Map<String, Object> runtimeConfig) {
        Object knowledge = runtimeConfig.get("knowledge");
        boolean enabled = knowledge instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("enabled"));
        if (!enabled) {
            return List.of(Map.of(
                    "title", "本地知识库未启用",
                    "summary", "当前仅基于照片状态、上下文和模型判断生成调色方向。"
            ));
        }
        return List.of(Map.of(
                "title", "知识库检索占位",
                "summary", "已启用知识库配置，后续可接入管理端素材检索结果。"
        ));
    }

    private String mergeMessage(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        return first + "\n\n" + second;
    }

    private String sessionId(Map<String, Object> payload) {
        Object value = payload.get("sessionId");
        if (value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))) {
            return "local-session-" + System.currentTimeMillis();
        }
        return String.valueOf(value);
    }
}
