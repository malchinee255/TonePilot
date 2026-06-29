package com.tonepilot.runtime.bridge;

import com.tonepilot.runtime.config.RuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class LightroomToolService {

    private final RuntimeProperties properties;
    private final LightroomStateService stateService;

    @Autowired
    public LightroomToolService(RuntimeProperties properties, LightroomStateService stateService) {
        this.properties = properties;
        this.stateService = stateService;
    }

    public Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings) {
        if (developSettings == null || developSettings.isEmpty()) {
            return Map.of("success", true, "message", "没有需要应用的 Lightroom 参数。");
        }
        try {
            BridgePaths paths = stateService.bridgePaths();
            Files.createDirectories(paths.fs("apply-jobs"));
            Files.createDirectories(paths.fs("apply-results"));
            Files.createDirectories(paths.fs("results"));
            String jobId = "agent-apply-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
            String resultFileName = jobId + ".result";
            String previewFileName = jobId + ".jpg";
            Path resultPath = paths.fs("apply-results", resultFileName);
            Path jobPath = paths.fs("apply-jobs", jobId + ".lua");
            Files.writeString(jobPath, toLuaTable(Map.of(
                    "id", jobId,
                    "resultPath", paths.lightroom("apply-results", resultFileName),
                    "previewFileName", previewFileName,
                    "previewPath", paths.lightroom("results", previewFileName),
                    "developSettings", developSettings
            )));
            return waitForResult(resultPath, Duration.ofMillis(properties.getBridge().getApplyTimeoutMs()));
        } catch (Exception exception) {
            return Map.of("success", false, "message", "写入 Lightroom 调色任务失败：" + exception.getMessage());
        }
    }

    private Map<String, Object> waitForResult(Path resultPath, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(resultPath)) {
                return parseResult(resultPath);
            }
            Thread.sleep(500);
        }
        return Map.of("success", false, "message", "等待 Lightroom 插件应用参数超时。");
    }

    private Map<String, Object> parseResult(Path resultPath) {
        try {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            for (String line : Files.readAllLines(resultPath)) {
                int index = line.indexOf('=');
                if (index > 0) {
                    result.put(line.substring(0, index), line.substring(index + 1));
                }
            }
            return Map.of(
                    "success", "true".equals(String.valueOf(result.get("success"))),
                    "message", String.valueOf(result.getOrDefault("message", "")),
                    "previewUrl", String.valueOf(result.getOrDefault("previewUrl", ""))
            );
        } catch (Exception exception) {
            return Map.of("success", false, "message", "读取 Lightroom 应用结果失败：" + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String toLuaTable(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                builder.append("[\"").append(escapeLua(String.valueOf(entry.getKey()))).append("\"]=")
                        .append(toLuaTable(entry.getValue()))
                        .append(",");
            }
            builder.append("}");
            return builder.toString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + escapeLua(String.valueOf(value == null ? "" : value)) + "\"";
    }

    private String escapeLua(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
