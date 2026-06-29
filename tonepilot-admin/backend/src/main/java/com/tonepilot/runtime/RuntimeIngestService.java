package com.tonepilot.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.persistence.PersistenceProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuntimeIngestService {

    private final List<RuntimeEventRecord> localEvents = new ArrayList<>();

    @Autowired
    private PersistenceProperties persistenceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public synchronized RuntimeDeviceRegistrationResponse registerDevice(RuntimeDeviceRegistrationRequest request) {
        String fingerprint = required(request.fingerprint(), "fingerprint");
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!persistenceProperties.isEnabled() || jdbcTemplate == null) {
            String userId = "local-user-" + stableId(fingerprint);
            String deviceId = "local-device-" + stableId(fingerprint);
            return new RuntimeDeviceRegistrationResponse(userId, deviceId, true);
        }

        List<RuntimeDeviceRegistrationResponse> existing = jdbcTemplate.query("""
                        SELECT user_id, id
                        FROM runtime_device
                        WHERE fingerprint = ?
                        """,
                (rs, rowNum) -> new RuntimeDeviceRegistrationResponse(
                        rs.getString("user_id"),
                        rs.getString("id"),
                        false
                ),
                fingerprint
        );
        if (!existing.isEmpty()) {
            updateDeviceHeartbeat(jdbcTemplate, existing.get(0).deviceId(), request);
            return existing.get(0);
        }

        Instant now = Instant.now();
        String userId = "usr_" + UUID.randomUUID().toString().replace("-", "");
        String deviceId = "dev_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                        INSERT INTO runtime_user (id, display_name, source, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                userId,
                "Lightroom 用户",
                "local-runtime",
                Timestamp.from(now),
                Timestamp.from(now)
        );
        jdbcTemplate.update("""
                        INSERT INTO runtime_device (
                            id, user_id, fingerprint, device_name, endpoint, metadata_json, last_seen_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                deviceId,
                userId,
                fingerprint,
                defaultText(request.deviceName(), "TonePilot Local Runtime"),
                defaultText(request.endpoint(), ""),
                toJson(request.metadata()),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return new RuntimeDeviceRegistrationResponse(userId, deviceId, true);
    }

    public synchronized RuntimeEventRecord recordEvent(RuntimeEventRequest request) {
        String userId = required(request.userId(), "userId");
        String deviceId = required(request.deviceId(), "deviceId");
        String eventType = required(request.eventType(), "eventType");
        RuntimeEventRecord record = new RuntimeEventRecord(
                "evt_" + UUID.randomUUID().toString().replace("-", ""),
                userId,
                deviceId,
                eventType,
                defaultText(request.sessionId(), ""),
                toJson(request.payload()),
                Instant.now()
        );
        localEvents.add(record);

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (persistenceProperties.isEnabled() && jdbcTemplate != null) {
            jdbcTemplate.update("""
                            INSERT INTO runtime_event (
                                id, user_id, device_id, event_type, session_id, payload_json, created_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    record.id(),
                    record.userId(),
                    record.deviceId(),
                    record.eventType(),
                    record.sessionId(),
                    record.payloadJson(),
                    Timestamp.from(record.createdAt())
            );
        }
        return record;
    }

    public synchronized List<RuntimeEventRecord> listEvents(String userId) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (persistenceProperties.isEnabled() && jdbcTemplate != null) {
            return jdbcTemplate.query("""
                            SELECT id, user_id, device_id, event_type, session_id, payload_json, created_at
                            FROM runtime_event
                            WHERE user_id = ?
                            ORDER BY created_at DESC
                            """,
                    (rs, rowNum) -> new RuntimeEventRecord(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getString("device_id"),
                            rs.getString("event_type"),
                            rs.getString("session_id"),
                            rs.getString("payload_json"),
                            rs.getTimestamp("created_at").toInstant()
                    ),
                    userId
            );
        }
        return localEvents.stream()
                .filter(event -> event.userId().equals(userId))
                .toList();
    }

    private void updateDeviceHeartbeat(
            JdbcTemplate jdbcTemplate,
            String deviceId,
            RuntimeDeviceRegistrationRequest request
    ) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                        UPDATE runtime_device
                        SET device_name = ?, endpoint = ?, metadata_json = ?, last_seen_at = ?, updated_at = ?
                        WHERE id = ?
                        """,
                defaultText(request.deviceName(), "TonePilot Local Runtime"),
                defaultText(request.endpoint(), ""),
                toJson(request.metadata()),
                Timestamp.from(now),
                Timestamp.from(now),
                deviceId
        );
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("运行时事件 JSON 序列化失败：" + exception.getMessage(), exception);
        }
    }

    private String required(String value, String name) {
        String text = defaultText(value, "");
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return text;
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String stableId(String value) {
        return Integer.toHexString(value.hashCode());
    }
}
