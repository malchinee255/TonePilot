package com.tonepilot.runtime.infrastructure.lightroom.repository;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;


import com.tonepilot.runtime.infrastructure.observability.RuntimeTraceLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.tonepilot.runtime.repository.lightroom.LightroomToolRepository;
import com.tonepilot.runtime.infrastructure.config.RuntimeProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Repository
public class FileLightroomToolRepository implements LightroomToolRepository {

    @Autowired
    private RuntimeProperties properties;

    @Autowired
    private RuntimeTraceLogger traceLogger;

    public Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings) {
        if (developSettings == null || developSettings.isEmpty()) {
            traceLogger.info("lightroom.apply.skipped", "", Map.of("reason", "empty_develop_settings"));
            return Map.of("success", true, "message", "没有需要应用的 Lightroom 参数。");
        }
        String jobId = "agent-apply-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        try {
            BridgePaths paths = new BridgePaths(properties);
            Files.createDirectories(paths.fs("apply-jobs"));
            Files.createDirectories(paths.fs("apply-results"));
            Files.createDirectories(paths.fs("results"));
            String resultFileName = jobId + ".result";
            String previewFileName = jobId + ".jpg";
            Path resultPath = paths.fs("apply-results", resultFileName);
            Path jobPath = paths.fs("apply-jobs", jobId + ".lua");
            traceLogger.info("lightroom.apply.job.writing", jobId, Map.of(
                    "settingCount", developSettings.size(),
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
            Map<String, Object> result = Map.of(
                    "success", true,
                    "pending", true,
                    "jobId", jobId,
                    "message", "已提交 Lightroom 调色任务，等待插件处理完成。",
                    "previewFileName", previewFileName
            );
            traceLogger.info("lightroom.apply.submitted", jobId, result);
            return result;
        } catch (Exception exception) {
            traceLogger.error("lightroom.apply.failed", jobId, Map.of("error", exception.getMessage()));
            return Map.of("success", false, "message", "写入 Lightroom 调色任务失败：" + exception.getMessage());
        }
    }

    public Map<String, Object> applyStatus(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Map.of("success", false, "message", "缺少 Lightroom 调色任务 ID。");
        }
        Path resultPath = new BridgePaths(properties).fs("apply-results", jobId + ".result");
        if (!Files.exists(resultPath)) {
            traceLogger.info("lightroom.apply.status.pending", jobId, Map.of("resultPath", resultPath.toString()));
            return Map.of(
                    "success", true,
                    "pending", true,
                    "jobId", jobId,
                    "message", "Lightroom 正在处理调色任务。"
            );
        }
        Map<String, Object> result = parseResult(jobId, resultPath);
        traceLogger.info("lightroom.apply.status.finished", jobId, result);
        return result;
    }

    private Map<String, Object> parseResult(String jobId, Path resultPath) {
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
                    "pending", false,
                    "jobId", jobId,
                    "message", String.valueOf(result.getOrDefault("message", "")),
                    "previewUrl", String.valueOf(result.getOrDefault("previewUrl", ""))
            );
        } catch (Exception exception) {
            traceLogger.error("lightroom.apply.result.read_failed", jobId, Map.of(
                    "resultPath", resultPath.toString(),
                    "error", exception.getMessage()
            ));
            return Map.of("success", false, "pending", false, "jobId", jobId, "message", "读取 Lightroom 应用结果失败：" + exception.getMessage());
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
