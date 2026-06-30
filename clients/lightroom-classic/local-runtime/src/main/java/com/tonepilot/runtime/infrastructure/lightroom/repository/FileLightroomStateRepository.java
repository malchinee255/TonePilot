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


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.runtime.infrastructure.config.RuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.tonepilot.runtime.repository.lightroom.LightroomStateRepository;

import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class FileLightroomStateRepository implements LightroomStateRepository {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuntimeProperties properties;

    public Map<String, Object> status() {
        Map<String, Object> heartbeat = readHeartbeat();
        if (heartbeat.isEmpty()) {
            return Map.of(
                    "available", false,
                    "mode", "lightroom-classic-local-runtime",
                    "message", "TonePilot Local Runtime 已启动，但 Lightroom 插件尚未写入心跳。"
            );
        }
        return Map.of(
                "available", true,
                "mode", "lightroom-classic-local-runtime",
                "message", "TonePilot Local Runtime 已连接 Lightroom 插件。",
                "heartbeat", heartbeat
        );
    }

    public Map<String, Object> selectedPhoto() {
        var path = bridgePaths().fs("selected-photo.json");
        if (!Files.exists(path)) {
            return Map.of("available", false, "message", "Lightroom 插件尚未写入当前照片状态。");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    Files.readString(path).replaceFirst("^\\uFEFF", ""),
                    new TypeReference<>() {
                    }
            );
            long updatedAt = longValue(payload.get("updatedAt"));
            long ageSeconds = Math.max(0, Instant.now().getEpochSecond() - updatedAt);
            Map<String, Object> result = new LinkedHashMap<>(payload);
            result.put("ageSeconds", ageSeconds);
            if (Files.exists(bridgePaths().fs("results", "selected-preview.jpg"))) {
                result.put("previewUrl", "/files/selected-preview.jpg?t=" + updatedAt);
            }
            return result;
        } catch (Exception exception) {
            return Map.of("available", false, "message", "读取 Lightroom 当前照片状态失败：" + exception.getMessage());
        }
    }

    public BridgePaths bridgePaths() {
        return new BridgePaths(properties);
    }

    @Override
    public java.nio.file.Path resultFile(String fileName) {
        return bridgePaths().fs("results", fileName).normalize();
    }

    private Map<String, Object> readHeartbeat() {
        var path = bridgePaths().fs("heartbeat.txt");
        if (!Files.exists(path)) {
            return Map.of();
        }
        try {
            long timestamp = Long.parseLong(Files.readString(path).trim());
            long ageSeconds = Instant.now().getEpochSecond() - timestamp;
            if (ageSeconds > 15) {
                return Map.of();
            }
            return Map.of("timestamp", timestamp, "ageSeconds", ageSeconds);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            return 0;
        }
    }
}
