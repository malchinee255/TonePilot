package com.tonepilot.infrastructure.lightroom.repository;

import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;





import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.infrastructure.config.RuntimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.tonepilot.repository.lightroom.LightroomStateRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
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
            String photoKey = photoKey(payload);
            if (!photoKey.isBlank()) {
                result.put("session", Map.of("photoKey", photoKey));
            }
            Path livePreview = bridgePaths().fs("results", "selected-preview.jpg");
            if (Files.exists(livePreview)) {
                String snapshotName = snapshotPreview(livePreview, photoKey, updatedAt);
                result.put("previewUrl", "/files/" + snapshotName + "?t=" + updatedAt);
                result.put("previewFileName", snapshotName);
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

    @SuppressWarnings("unchecked")
    private String photoKey(Map<String, Object> payload) {
        Object photoObject = payload.get("photo");
        if (photoObject instanceof Map<?, ?> photo) {
            Object pathValue = photo.get("path");
            String path = pathValue == null ? "" : String.valueOf(pathValue);
            if (!path.isBlank()) {
                return sha256(path);
            }
            Object fileNameValue = photo.get("fileName");
            String fileName = fileNameValue == null ? "" : String.valueOf(fileNameValue);
            if (!fileName.isBlank()) {
                return sha256(fileName);
            }
        }
        return "";
    }

    private String snapshotPreview(Path livePreview, String photoKey, long updatedAt) throws Exception {
        String key = photoKey == null || photoKey.isBlank() ? "unknown" : photoKey.substring(0, Math.min(12, photoKey.length()));
        String fileName = "selected-preview-" + key + "-" + updatedAt + ".jpg";
        Path snapshot = bridgePaths().fs("results", fileName);
        if (!Files.exists(snapshot)) {
            Files.copy(livePreview, snapshot, StandardCopyOption.REPLACE_EXISTING);
        }
        return fileName;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
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
