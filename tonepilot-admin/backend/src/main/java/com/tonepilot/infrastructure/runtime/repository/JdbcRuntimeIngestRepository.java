package com.tonepilot.infrastructure.runtime.repository;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.infrastructure.shared.persistence.PersistenceProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.tonepilot.repository.runtime.RuntimeIngestRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcRuntimeIngestRepository implements RuntimeIngestRepository {

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
