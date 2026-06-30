package com.tonepilot.infrastructure.shared.persistence;

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


import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class DomainSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(DomainSnapshotRepository.class);

    private final PersistenceProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    @Autowired
    public DomainSnapshotRepository(
            PersistenceProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public void save(String type, Object id, Object value) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!properties.isEnabled() || jdbcTemplate == null || id == null || value == null) {
            return;
        }
        try {
            String domainId = String.valueOf(id);
            String json = objectMapper.writeValueAsString(value);
            Instant now = Instant.now();
            int updated = jdbcTemplate.update("""
                            UPDATE domain_snapshot
                            SET payload_json = ?, updated_at = ?
                            WHERE domain_type = ? AND domain_id = ?
                            """,
                    json,
                    Timestamp.from(now),
                    type,
                    domainId
            );
            if (updated == 0) {
                jdbcTemplate.update("""
                                INSERT INTO domain_snapshot (
                                    domain_type, domain_id, payload_json, created_at, updated_at
                                ) VALUES (?, ?, ?, ?, ?)
                                """,
                        type,
                        domainId,
                        json,
                        Timestamp.from(now),
                        Timestamp.from(now)
                );
            }
        } catch (Exception exception) {
            log.debug("领域快照写入失败 type={} id={}：{}", type, id, exception.getMessage());
        }
    }

    public <T> Optional<T> find(String type, Object id, Class<T> targetType) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!properties.isEnabled() || jdbcTemplate == null || id == null) {
            return Optional.empty();
        }
        try {
            return jdbcTemplate.query(
                    "SELECT payload_json FROM domain_snapshot WHERE domain_type = ? AND domain_id = ?",
                    resultSet -> {
                        if (!resultSet.next()) {
                        return Optional.<T>empty();
                    }
                    return Optional.of(readPayload(resultSet.getString("payload_json"), targetType));
                },
                type,
                String.valueOf(id)
            );
        } catch (Exception exception) {
            log.debug("领域快照读取失败 type={} id={}：{}", type, id, exception.getMessage());
            return Optional.empty();
        }
    }

    public <T> List<T> list(String type, Class<T> targetType) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!properties.isEnabled() || jdbcTemplate == null) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    "SELECT payload_json FROM domain_snapshot WHERE domain_type = ?",
                    (resultSet, rowNum) -> readPayload(resultSet.getString("payload_json"), targetType),
                    type
            );
        } catch (Exception exception) {
            log.debug("领域快照列表读取失败 type={}：{}", type, exception.getMessage());
            return List.of();
        }
    }

    private <T> T readPayload(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception exception) {
            throw new IllegalArgumentException("领域快照 JSON 解析失败：" + exception.getMessage(), exception);
        }
    }
}


