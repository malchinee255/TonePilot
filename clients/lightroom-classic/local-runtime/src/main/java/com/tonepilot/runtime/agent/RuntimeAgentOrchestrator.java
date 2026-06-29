package com.tonepilot.runtime.agent;

import com.tonepilot.runtime.admin.AdminRuntimeClient;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.bridge.LightroomToolService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private RuleBasedRuntimeAgent ruleAgent;

    @Autowired
    private ModelRuntimeAgent modelAgent;

    @Autowired
    private RuntimeConfigService configService;

    @Autowired
    private AdminRuntimeClient adminRuntimeClient;

    @Autowired
    private RuntimeTraceLogger traceLogger;

    @Autowired
    private AgentConversationMemory memory;

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

        if (isConfirmToApply(message)) {
            return applyPendingDecision(sessionId, selected);
        }

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
        traceLogger.info("agent.decision.finished", sessionId, Map.of(
                "deltaCount", tuneResult.deltas().size(),
                "settingCount", tuneResult.developSettings().size(),
                "analysis", tuneResult.analysis()
        ));

        String beforePreviewUrl = String.valueOf(selected.getOrDefault("baselinePreviewUrl", selected.getOrDefault("previewUrl", "")));
        memory.rememberDecision(sessionId, new AgentConversationMemory.PendingDecision(
                sessionId,
                message,
                tuneResult,
                selected.get("photo") instanceof Map<?, ?> photo ? (Map<String, Object>) photo : Map.of(),
                beforePreviewUrl,
                Instant.now()
        ));

        Map<String, Object> data = baseData(sessionId, selected, tuneResult);
        data.put("action", "await_user_decision");
        data.put("beforePreviewUrl", beforePreviewUrl);
        data.put("knowledgeMatches", knowledgeMatches(runtimeConfig));
        data.put("suggestedReplies", List.of("确认，按这个修", "方向可以，但再自然一点", "先不要修，换一种风格"));
        data.put("assistantMessage", tuneResult.assistantMessage()
                + "\n\n如果这个方向合适，直接回复“确认按这个修”；如果不合适，继续告诉我你想调整的方向。");

        adminRuntimeClient.recordEvent("agent.decision.proposed", sessionId, data);
        traceLogger.info("agent.response.ready", sessionId, Map.of("action", "await_user_decision"));
        return Map.of("success", true, "message", data.get("assistantMessage"), "data", data);
    }

    public Map<String, Object> applyStatus(String jobId) {
        return toolService.applyStatus(jobId);
    }

    private Map<String, Object> applyPendingDecision(String sessionId, Map<String, Object> selected) {
        var pending = memory.pendingDecision(sessionId);
        if (pending.isEmpty()) {
            traceLogger.warn("agent.apply.no_pending_decision", sessionId, Map.of());
            return Map.of(
                    "success", true,
                    "message", "我还没有可以执行的调色方案。请先描述你想要的效果，我会先分析方向。",
                    "data", Map.of("sessionId", sessionId, "action", "need_user_intent")
            );
        }

        AgentConversationMemory.PendingDecision decision = pending.get();
        AgentTuneResult tuneResult = decision.tuneResult();
        Map<String, Object> applyResult = toolService.applyDevelopSettings(tuneResult.developSettings());
        memory.clearPendingDecision(sessionId);

        Map<String, Object> data = baseData(sessionId, selected, tuneResult);
        data.put("action", "tool_submitted");
        data.put("apply", applyResult);
        data.put("beforePreviewUrl", decision.beforePreviewUrl());
        data.put("afterPreviewUrl", "");
        data.put("assistantMessage", "我已经把确认后的方案提交给 Lightroom，处理完成后会更新修图后预览。");

        adminRuntimeClient.recordEvent("agent.tool.submitted", sessionId, data);
        traceLogger.info("agent.tool.submitted", sessionId, Map.of("apply", applyResult));
        return Map.of("success", applyResult.getOrDefault("success", true), "message", data.get("assistantMessage"), "data", data);
    }

    private Map<String, Object> baseData(String sessionId, Map<String, Object> selected, AgentTuneResult tuneResult) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("photo", selected.getOrDefault("photo", Map.of()));
        data.put("analysis", tuneResult.analysis());
        data.put("assistantMessage", tuneResult.assistantMessage());
        data.put("deltas", tuneResult.deltas());
        data.put("developSettings", tuneResult.developSettings());
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

    private boolean isConfirmToApply(String message) {
        String value = message == null ? "" : message.trim().toLowerCase();
        return value.contains("确认")
                || value.contains("按这个修")
                || value.contains("开始修")
                || value.contains("执行")
                || value.contains("apply")
                || value.contains("go ahead");
    }

    private String sessionId(Map<String, Object> payload) {
        Object value = payload.get("sessionId");
        if (value == null || String.valueOf(value).isBlank() || "null".equals(String.valueOf(value))) {
            return "local-session-" + System.currentTimeMillis();
        }
        return String.valueOf(value);
    }
}
