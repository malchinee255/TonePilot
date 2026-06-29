package com.tonepilot.runtime.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.bridge.BridgePaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RuntimeConfigService {

    private final ObjectMapper objectMapper;
    private final BridgePaths bridgePaths;

    @Autowired
    public RuntimeConfigService(ObjectMapper objectMapper, RuntimeProperties properties) {
        this.objectMapper = objectMapper;
        this.bridgePaths = new BridgePaths(properties);
    }

    public Map<String, Object> readPublicConfig() {
        Map<String, Object> config = readConfig();
        hideApiKey(config, "openai");
        hideApiKey(config, "qwen2");
        return config;
    }

    public Map<String, Object> writeConfig(Map<String, Object> patch) {
        Map<String, Object> current = readConfig();
        merge(current, patch == null ? Map.of() : patch);
        try {
            Files.createDirectories(bridgePaths.fsRoot());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(bridgePaths.fs("runtime-config.json").toFile(), current);
        } catch (Exception exception) {
            throw new IllegalArgumentException("保存本地运行时配置失败：" + exception.getMessage(), exception);
        }
        return readPublicConfig();
    }

    private Map<String, Object> readConfig() {
        Map<String, Object> defaults = defaultConfig();
        var path = bridgePaths.fs("runtime-config.json");
        if (!Files.exists(path)) {
            return defaults;
        }
        try {
            Map<String, Object> file = objectMapper.readValue(Files.readString(path), new TypeReference<>() {
            });
            merge(defaults, file);
            return defaults;
        } catch (Exception exception) {
            return defaults;
        }
    }

    @SuppressWarnings("unchecked")
    private void merge(Map<String, Object> target, Map<String, Object> patch) {
        patch.forEach((key, value) -> {
            if (value instanceof Map<?, ?> map && target.get(key) instanceof Map<?, ?> targetMap) {
                merge((Map<String, Object>) targetMap, (Map<String, Object>) map);
            } else {
                target.put(key, value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void hideApiKey(Map<String, Object> config, String provider) {
        if (config.get(provider) instanceof Map<?, ?> map) {
            Map<String, Object> providerConfig = (Map<String, Object>) map;
            boolean configured = providerConfig.get("apiKey") != null && !String.valueOf(providerConfig.get("apiKey")).isBlank();
            providerConfig.remove("apiKey");
            providerConfig.put("apiKeyConfigured", configured);
        }
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("provider", "rule");
        config.put("openai", new LinkedHashMap<>(Map.of(
                "apiKey", "",
                "baseUrl", "https://api.openai.com/v1",
                "model", "gpt-4o-mini"
        )));
        config.put("qwen2", new LinkedHashMap<>(Map.of(
                "apiKey", "",
                "baseUrl", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "model", "qwen-plus"
        )));
        config.put("knowledge", new LinkedHashMap<>(Map.of("enabled", false, "syncUrl", "")));
        return config;
    }
}
