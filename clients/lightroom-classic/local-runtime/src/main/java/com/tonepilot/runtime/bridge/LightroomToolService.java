package com.tonepilot.runtime.bridge;

import com.tonepilot.runtime.config.RuntimeProperties;
import com.tonepilot.runtime.observability.RuntimeTraceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class LightroomToolService {

    @Autowired
    private RuntimeProperties properties;

    @Autowired
    private LightroomStateService stateService;

    @Autowired
    private RuntimeTraceLogger traceLogger;

    public Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings) {
        if (developSettings == null || developSettings.isEmpty()) {
            traceLogger.info("lightroom.apply.skipped", "", Map.of("reason", "empty_develop_settings"));
            return Map.of("success", true, "message", "没有需要应用的 Lightroom 参数。");
        }
        String jobId = "agent-apply-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        try {
            BridgePaths paths = stateService.bridgePaths();
            traceLogger.info("lightroom.apply.prepare", jobId, Map.of(
                    "settingCount", developSettings.size(),
                    "bridgeRoot", paths.fsRoot().toString()
            ));
            Files.createDirectories(paths.fs("apply-jobs"));
            Files.createDirectories(paths.fs("apply-results"));
            Files.createDirectories(paths.fs("results"));
            String resultFileName = jobId + ".result";
            String previewFileName = jobId + ".jpg";
            Path resultPath = paths.fs("apply-results", resultFileName);
            Path jobPath = paths.fs("apply-jobs", jobId + ".lua");
            traceLogger.info("lightroom.apply.job.writing", jobId, Map.of(
                    "jobPath", jobPath.toString(),
                    "resultPath", resultPath.toString(),
                    "previewFileName", previewFileName
            ));
            Files.writeString(jobPath, "return " + toLuaTable(Map.of(
                    "id", jobId,
                    "resultPath", paths.lightroom("apply-results", resultFileName),
                    "previewFileName", previewFileName,
                    "previewPath", paths.lightroom("results", previewFileName),
                    "developSettings", developSettings
            )));
            traceLogger.info("lightroom.apply.job.written", jobId, Map.of("jobPath", jobPath.toString()));
            Map<String, Object> result = waitForResult(jobId, resultPath, Duration.ofMillis(properties.getBridge().getApplyTimeoutMs()));
            traceLogger.info("lightroom.apply.finished", jobId, result);
            return result;
        } catch (Exception exception) {
            traceLogger.error("lightroom.apply.failed", jobId, Map.of("error", exception.getMessage()));
            return Map.of("success", false, "message", "写入 Lightroom 调色任务失败：" + exception.getMessage());
        }
    }

    private Map<String, Object> waitForResult(String jobId, Path resultPath, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        traceLogger.info("lightroom.apply.waiting", jobId, Map.of(
                "resultPath", resultPath.toString(),
                "timeoutMs", timeout.toMillis()
        ));
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(resultPath)) {
                return parseResult(jobId, resultPath);
            }
            Thread.sleep(500);
        }
        traceLogger.warn("lightroom.apply.timeout", jobId, Map.of(
                "resultPath", resultPath.toString(),
                "timeoutMs", timeout.toMillis()
        ));
        return Map.of("success", false, "message", "等待 Lightroom 插件应用参数超时。");
    }

    private Map<String, Object> parseResult(String jobId, Path resultPath) {
        try {
            traceLogger.info("lightroom.apply.result.reading", jobId, Map.of("resultPath", resultPath.toString()));
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            for (String line : Files.readAllLines(resultPath)) {
                int index = line.indexOf('=');
                if (index > 0) {
                    result.put(line.substring(0, index), line.substring(index + 1));
                }
            }
            Map<String, Object> parsed = Map.of(
                    "success", "true".equals(String.valueOf(result.get("success"))),
                    "message", String.valueOf(result.getOrDefault("message", "")),
                    "previewUrl", String.valueOf(result.getOrDefault("previewUrl", ""))
            );
            traceLogger.info("lightroom.apply.result.parsed", jobId, parsed);
            return parsed;
        } catch (Exception exception) {
            traceLogger.error("lightroom.apply.result.read_failed", jobId, Map.of(
                    "resultPath", resultPath.toString(),
                    "error", exception.getMessage()
            ));
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
