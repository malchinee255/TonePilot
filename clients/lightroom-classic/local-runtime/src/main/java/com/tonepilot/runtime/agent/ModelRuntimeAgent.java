package com.tonepilot.runtime.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ModelRuntimeAgent {

    private static final Logger log = LoggerFactory.getLogger(ModelRuntimeAgent.class);

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RuntimeTraceLogger traceLogger;

    private HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ModelRuntimeAgent() {
    }

    public AgentTuneResult plan(
            AgentInput input,
            String provider,
            Map<String, Object> runtimeConfig
    ) {
        if (provider == null || provider.isBlank() || "rule".equals(provider)) {
            throw new IllegalStateException("请先选择 OpenAI 或阿里 Qwen2 模型，本地规则模式已移除。");
        }
        ProviderConfig config = providerConfig(provider, runtimeConfig);
        if (!config.ready()) {
            traceLogger.warn("model.provider.not_ready", "", Map.of("provider", provider));
            throw new IllegalStateException("模型配置不完整，请在左侧“模型设置”中填写 Base URL、模型名称和 API Key。");
        }
        try {
            traceLogger.info("model.request.sending", "", Map.of(
                    "provider", provider,
                    "baseUrl", config.baseUrl(),
                    "model", config.model()
            ));
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", config.model(),
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(input))
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(config.baseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            traceLogger.info("model.response.received", "", Map.of(
                    "provider", provider,
                    "statusCode", response.statusCode()
            ));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("{} 模型调用失败，状态码：{}", provider, response.statusCode());
                traceLogger.warn("model.response.non_success", "", Map.of(
                        "provider", provider,
                        "statusCode", response.statusCode()
                ));
                throw new IllegalStateException("模型调用失败，HTTP 状态码：" + response.statusCode());
            }
            return parseModelResult(input, response.body());
        } catch (Exception exception) {
            log.debug("{} 模型调用失败：{}", provider, exception.getMessage());
            traceLogger.warn("model.request.failed", "", Map.of(
                    "provider", provider,
                    "error", exception.getMessage()
            ));
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("模型调用失败：" + exception.getMessage(), exception);
        }
    }

    private AgentTuneResult parseModelResult(
            AgentInput input,
            String responseBody
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
            if (developSettings == null || developSettings.isEmpty()) {
                traceLogger.info("model.parse.analysis_only", "", Map.of(
                        "hasAnalysis", json.has("analysis"),
                        "localAdjustmentCount", localAdjustments.size()
                ));
                return new AgentTuneResult(
                        json.path("assistantMessage").asText("我已经完成照片分析，本轮不需要修改 Lightroom 全局参数。"),
                        Map.of(),
                        List.of(),
                        analysis,
                        localAdjustments,
                        content
                );
            }
            traceLogger.info("model.parse.succeeded", "", Map.of(
                    "settingCount", developSettings.size(),
                    "localAdjustmentCount", localAdjustments.size(),
                    "hasAnalysis", json.has("analysis")
            ));
            return new AgentTuneResult(
                    json.path("assistantMessage").asText("已根据模型结果生成 Lightroom 调色参数。"),
                    developSettings,
                    buildDeltas(input.currentSettings(), developSettings),
                    analysis,
                    localAdjustments,
                    content
            );
        } catch (Exception exception) {
            log.debug("模型响应解析失败：{}", exception.getMessage());
            traceLogger.warn("model.parse.failed", "", Map.of("error", exception.getMessage()));
            throw new IllegalStateException("模型响应解析失败：" + exception.getMessage(), exception);
        }
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
                developSettings 只输出本轮需要真实应用的全局 Lightroom Develop Settings，未被用户明确要求或你明确规划的参数不要出现。
                如果用户只要求分析而不要求修图，则 developSettings 输出空对象。
                localAdjustments 用来描述局部蒙版计划，当前只作为计划展示，不会直接写入 Lightroom 蒙版。
                localAdjustments 的 region 必须使用 normalized_crop 坐标，x/y/w/h/centerX/centerY/radius 都在 0 到 1 之间。
                JSON 结构：
                {
                  "assistantMessage": "中文解释",
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
                + "\n\n当前 Lightroom 参数：\n" + safeJson(input.currentSettings());
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
