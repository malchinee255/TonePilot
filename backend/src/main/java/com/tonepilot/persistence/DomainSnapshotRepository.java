package com.tonepilot.persistence;

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


