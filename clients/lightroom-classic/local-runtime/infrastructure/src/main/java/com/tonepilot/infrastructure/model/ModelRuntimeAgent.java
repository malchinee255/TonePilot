package com.tonepilot.infrastructure.model;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;





import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.infrastructure.observability.RuntimeTraceLogger;
import com.tonepilot.infrastructure.config.RuntimeProperties;
import com.tonepilot.infrastructure.lightroom.filesystem.BridgePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class ModelRuntimeAgent {

    private static final Logger log = LoggerFactory.getLogger(ModelRuntimeAgent.class);

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RuntimeTraceLogger traceLogger;

    @Autowired
    private RuntimeProperties properties = new RuntimeProperties();

    private HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ModelRuntimeAgent() {
    }

    public AgentTuneResult plan(
            AgentInput input,
            String provider,
            Map<String, Object> runtimeConfig,
            String sessionId
    ) {
        if (provider == null || provider.isBlank() || "rule".equals(provider)) {
            throw new IllegalStateException("请先选择 OpenAI 或阿里 Qwen2 模型，本地规则模式已移除。");
        }
        ProviderConfig config = providerConfig(provider, runtimeConfig);
        if (!config.ready()) {
            traceLogger.warn("model.provider.not_ready", sessionId, Map.of("provider", provider));
            throw new IllegalStateException("模型配置不完整，请在左侧“模型设置”中填写 Base URL、模型名称和 API Key。");
        }
        try {
            String systemPrompt = systemPrompt();
            String userPrompt = userPrompt(input);
            String imageDataUrl = supportsVisionModel(provider, config.model()) ? previewDataUrl(input.previewUrl()) : "";
            traceLogger.info("model.request.sending", sessionId, Map.of(
                    "provider", provider,
                    "baseUrl", config.baseUrl(),
                    "model", config.model(),
                    "userMessage", input.message() == null ? "" : input.message(),
                    "userPrompt", userPrompt,
                    "hasPhotoMetadata", input.photoMetadata() != null && !input.photoMetadata().isEmpty(),
                    "hasPreviewImage", !imageDataUrl.isBlank()
            ));
            String body = objectMapper.writeValueAsString(buildChatRequestPayload(
                    provider,
                    config.model(),
                    systemPrompt,
                    userPrompt,
                    imageDataUrl
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(config.baseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            traceLogger.info("model.response.received", sessionId, Map.of(
                    "provider", provider,
                    "statusCode", response.statusCode(),
                    "responseBody", response.body() == null ? "" : response.body()
            ));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("{} 模型调用失败，状态码：{}", provider, response.statusCode());
                traceLogger.warn("model.response.non_success", sessionId, Map.of(
                        "provider", provider,
                        "statusCode", response.statusCode()
                ));
                throw new IllegalStateException("模型调用失败，HTTP 状态码：" + response.statusCode());
            }
            return parseModelResult(input, response.body(), sessionId);
        } catch (Exception exception) {
            log.debug("{} 模型调用失败：{}", provider, exception.getMessage());
            traceLogger.warn("model.request.failed", sessionId, Map.of(
                    "provider", provider,
                    "error", exception.getMessage()
            ));
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("模型调用失败：" + exception.getMessage(), exception);
        }
    }

    private Map<String, Object> buildChatRequestPayload(
            String provider,
            String model,
            String systemPrompt,
            String userPrompt
    ) {
        return buildChatRequestPayload(provider, model, systemPrompt, userPrompt, "");
    }

    private Map<String, Object> buildChatRequestPayload(
            String provider,
            String model,
            String systemPrompt,
            String userPrompt,
            String imageDataUrl
    ) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 1600);
        Object userContent = imageDataUrl == null || imageDataUrl.isBlank()
                ? userPrompt
                : List.of(
                Map.of("type", "text", "text", userPrompt),
                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))
        );
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        if ("qwen2".equalsIgnoreCase(provider)) {
            payload.put("enable_thinking", false);
        }
        return payload;
    }

    private AgentTuneResult parseModelResult(
            AgentInput input,
            String responseBody,
            String sessionId
    ) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode json = objectMapper.readTree(extractJson(content));
            Map<String, Object> developSettings = objectMapper.convertValue(
                    json.path("developSettings"),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            List<Map<String, Object>> localAdjustments = json.has("localAdjustments")
                    ? objectMapper.convertValue(json.path("localAdjustments"), new TypeReference<>() {
                    })
                    : List.of();
            Map<String, Object> analysis = json.has("analysis")
                    ? objectMapper.convertValue(json.path("analysis"), new TypeReference<>() {
                    })
                    : Map.of("intent", input.message(), "photoType", "当前 Lightroom 照片", "recommendedStyle", "由模型分析生成");
            AgentThought agentThought = parseAgentThought(json, analysis);
            if (developSettings == null || developSettings.isEmpty()) {
                traceLogger.info("model.parse.analysis_only", sessionId, Map.of(
                        "hasAnalysis", json.has("analysis"),
                        "localAdjustmentCount", localAdjustments.size(),
                        "hasAgentThought", json.has("agentThought")
                ));
                return new AgentTuneResult(
                        json.path("assistantMessage").asText("我已经完成照片分析，本轮不需要修改 Lightroom 全局参数。"),
                        agentThought,
                        Map.of(),
                        List.of(),
                        analysis,
                        localAdjustments,
                        content
                );
            }
            traceLogger.info("model.parse.succeeded", sessionId, Map.of(
                    "settingCount", developSettings.size(),
                    "localAdjustmentCount", localAdjustments.size(),
                    "hasAnalysis", json.has("analysis"),
                    "hasAgentThought", json.has("agentThought")
            ));
            return new AgentTuneResult(
                    json.path("assistantMessage").asText("已根据模型结果生成 Lightroom 调色参数。"),
                    agentThought,
                    developSettings,
                    buildDeltas(input.currentSettings(), developSettings),
                    analysis,
                    localAdjustments,
                    content
            );
        } catch (Exception exception) {
            log.debug("模型响应解析失败：{}", exception.getMessage());
            traceLogger.warn("model.parse.failed", sessionId, Map.of("error", exception.getMessage()));
            throw new IllegalStateException("模型响应解析失败：" + exception.getMessage(), exception);
        }
    }

    private AgentThought parseAgentThought(JsonNode json, Map<String, Object> analysis) {
        if (!json.has("agentThought") || !json.path("agentThought").isObject()) {
            return new AgentThought(
                    String.valueOf(analysis.getOrDefault("recommendedStyle", "已完成当前照片判断。")),
                    List.of(String.valueOf(analysis.getOrDefault("photoType", "当前 Lightroom 照片"))),
                    String.valueOf(analysis.getOrDefault("intent", "根据用户输入和当前照片状态判断下一步。")),
                    "respond",
                    "等待用户确认或继续输入微调要求",
                    List.of(),
                    List.of("确认按这个方向修", "先不要修，只看分析")
            );
        }
        AgentThought thought = objectMapper.convertValue(json.path("agentThought"), AgentThought.class);
        return new AgentThought(
                stringOrDefault(thought.summary(), String.valueOf(analysis.getOrDefault("recommendedStyle", ""))),
                thought.observations() == null ? List.of() : thought.observations(),
                stringOrDefault(thought.reasoningVisible(), String.valueOf(analysis.getOrDefault("intent", ""))),
                stringOrDefault(thought.decision(), "respond"),
                stringOrDefault(thought.nextAction(), "等待用户继续确认"),
                thought.toolPlan() == null ? List.of() : thought.toolPlan(),
                thought.userOptions() == null ? List.of() : thought.userOptions()
        );
    }

    private List<AgentDelta> buildDeltas(Map<String, Object> current, Map<String, Object> developSettings) {
        return developSettings.entrySet().stream()
                .map(entry -> new AgentDelta(
                        "model",
                        entry.getKey(),
                        entry.getKey(),
                        current == null ? 0 : current.getOrDefault(entry.getKey(), 0),
                        entry.getValue(),
                        entry.getValue(),
                        "由用户选择的大模型生成，未输出的参数保持不变。"
                ))
                .toList();
    }

    private String systemPrompt() {
        return """
                你是 TonePilot 的 Lightroom 调色 Agent。
                只允许输出严格 JSON，不要输出 Markdown。
                你需要像主 Agent 一样先观察用户意图、当前照片调色状态和可用工具，再决定下一步动作。
                agentThought 是展示给用户看的阶段性判断结果，必须简洁、可解释，不要输出隐藏推理链或冗长思维过程。
                decision 只能是 respond、ask_user、apply_global_adjustments、apply_local_adjustments、plan_local_adjustments 之一。
                developSettings 只输出本轮需要真实应用的全局 Lightroom Develop Settings，未被用户明确要求或你明确规划的参数不要出现。
                如果 decision 不是 apply_global_adjustments 且没有全局参数需要和局部工具一起应用，则 developSettings 输出空对象。
                localAdjustments 用来描述局部蒙版计划；如果用户明确要求应用局部区域调整，decision 使用 apply_local_adjustments，本地插件会打开对应 Lightroom 局部工具并设置局部参数，但现代蒙版区域仍可能需要用户在 Lightroom 中确认或拖拽。
                如果只是分析局部修图方案、不应调用 Lightroom 工具，decision 使用 plan_local_adjustments。
                localAdjustments 的 region 必须使用 normalized_crop 坐标，x/y/w/h/centerX/centerY/radius 都在 0 到 1 之间。
                JSON 结构：
                {
                  "assistantMessage": "中文解释",
                  "agentThought": {
                    "summary": "主 Agent 对本轮请求的简短判断",
                    "observations": ["看到的关键画面/参数/意图线索"],
                    "reasoningVisible": "面向用户可展示的判断依据，不要写隐藏推理链",
                    "decision": "respond|ask_user|apply_global_adjustments|apply_local_adjustments|plan_local_adjustments",
                    "nextAction": "下一步准备做什么",
                    "toolPlan": ["如果要调用工具，列出准备做的动作"],
                    "userOptions": ["用户可以继续点击或输入的选项"]
                  },
                  "analysis": {"intent":"", "photoType":"", "recommendedStyle":""},
                  "developSettings": {"Exposure2012": 0.2},
                  "localAdjustments": [
                    {
                      "type": "linear_gradient|radial_gradient|brush|ai_subject|ai_sky",
                      "target": "天空",
                      "coordinateSpace": "normalized_crop",
                      "region": {"x": 0.0, "y": 0.0, "w": 1.0, "h": 0.42},
                      "feather": 0.65,
                      "settings": {"Exposure2012": -0.25, "Highlights2012": -18},
                      "reason": "压暗天空并保留城市灯光层次"
                    }
                  ]
                }
                """;
    }

    private String userPrompt(AgentInput input) {
        return "用户意图：\n" + input.message()
                + "\n\n当前照片元数据：\n" + safeJson(input.photoMetadata())
                + "\n\n当前照片预览：\n" + stringValue(input.previewUrl())
                + "\n\n当前 Lightroom 参数：\n" + safeJson(input.currentSettings())
                + "\n\n管理端知识库匹配：\n" + safeJson(input.knowledgeMatches())
                + "\n\n如果消息中附带了 image_url，请优先观察照片内容；如果没有附带图片，则只能基于元数据、当前参数、用户意图和知识库判断，并要说明不确定性。";
    }

    private boolean supportsVisionModel(String provider, String model) {
        String value = (model == null ? "" : model).toLowerCase();
        if (value.contains("vl") || value.contains("vision") || value.contains("omni")) {
            return true;
        }
        if ("qwen2".equalsIgnoreCase(provider)) {
            return value.contains("qwen3.6-plus");
        }
        if ("openai".equalsIgnoreCase(provider)) {
            return value.contains("gpt-4o") || value.contains("gpt-4.1") || value.contains("gpt-5") || value.contains("o4");
        }
        return false;
    }

    private String previewDataUrl(String previewUrl) {
        try {
            String value = stringValue(previewUrl);
            if (value.isBlank()) {
                return "";
            }
            if (value.startsWith("data:image/")) {
                return value;
            }
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return value;
            }
            if (!value.startsWith("/files/")) {
                return "";
            }
            String fileName = value.substring("/files/".length()).replaceFirst("\\?.*$", "");
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            BridgePaths paths = new BridgePaths(properties);
            Path resultsDir = paths.fs("results").normalize();
            Path file = resultsDir.resolve(fileName).normalize();
            if (!file.startsWith(resultsDir) || !Files.exists(file) || Files.size(file) > 5_000_000) {
                return "";
            }
            String mime = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        } catch (Exception exception) {
            return "";
        }
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private ProviderConfig providerConfig(String provider, Map<String, Object> runtimeConfig) {
        Object raw = runtimeConfig == null ? null : runtimeConfig.get(provider);
        if (!(raw instanceof Map<?, ?> map)) {
            return ProviderConfig.empty();
        }
        Map<String, Object> value = (Map<String, Object>) map;
        return new ProviderConfig(
                stringValue(value.get("apiKey")),
                stringValue(value.get("baseUrl")),
                stringValue(value.get("model"))
        );
    }

    private String extractJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "").replaceFirst("```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String stringOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trimSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private record ProviderConfig(String apiKey, String baseUrl, String model) {
        static ProviderConfig empty() {
            return new ProviderConfig("", "", "");
        }

        boolean ready() {
            return !apiKey.isBlank() && !baseUrl.isBlank() && !model.isBlank();
        }
    }
}
