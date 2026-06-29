package com.tonepilot.runtime.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class ModelRuntimeAgent {

    private static final Logger log = LoggerFactory.getLogger(ModelRuntimeAgent.class);

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RuleBasedRuntimeAgent ruleAgent = new RuleBasedRuntimeAgent();

    private HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ModelRuntimeAgent() {
    }

    public AgentTuneResult plan(
            AgentInput input,
            String provider,
            Map<String, Object> runtimeConfig,
            Supplier<AgentTuneResult> fallback
    ) {
        if (provider == null || provider.equals("rule")) {
            return fallback.get();
        }
        ProviderConfig config = providerConfig(provider, runtimeConfig);
        if (!config.ready()) {
            return fallback.get();
        }
        try {
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
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("{} 模型调用失败，状态码：{}", provider, response.statusCode());
                return fallback.get();
            }
            return parseModelResult(input, response.body(), fallback);
        } catch (Exception exception) {
            log.debug("{} 模型调用失败，回退本地规则：{}", provider, exception.getMessage());
            return fallback.get();
        }
    }

    @SuppressWarnings("unchecked")
    private AgentTuneResult parseModelResult(
            AgentInput input,
            String responseBody,
            Supplier<AgentTuneResult> fallback
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
            if (developSettings == null || developSettings.isEmpty()) {
                return fallback.get();
            }
            AgentTuneResult ruleShape = ruleAgent.plan(new AgentInput(input.message(), input.currentSettings()));
            Map<String, Object> analysis = json.has("analysis")
                    ? objectMapper.convertValue(json.path("analysis"), new TypeReference<>() {
                    })
                    : ruleShape.analysis();
            return new AgentTuneResult(
                    json.path("assistantMessage").asText("已根据模型结果生成 Lightroom 调色参数。"),
                    developSettings,
                    buildDeltas(input.currentSettings(), developSettings),
                    analysis
            );
        } catch (Exception exception) {
            log.debug("模型响应解析失败，回退本地规则：{}", exception.getMessage());
            return fallback.get();
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
                只输出本轮需要修改的 Lightroom Develop Settings，未被用户明确要求或你明确规划的参数不要出现。
                JSON 结构：
                {
                  "assistantMessage": "中文解释",
                  "analysis": {"intent":"", "photoType":"", "recommendedStyle":""},
                  "developSettings": {"Exposure2012": 0.2}
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
