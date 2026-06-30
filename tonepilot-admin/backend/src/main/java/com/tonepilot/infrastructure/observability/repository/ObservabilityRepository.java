package com.tonepilot.infrastructure.observability.repository;

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

import com.tonepilot.repository.observability.ObservabilityLogRepository;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.infrastructure.shared.persistence.PersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

@Component
public class ObservabilityRepository implements ObservabilityLogRepository {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityRepository.class);

    private final PersistenceProperties persistenceProperties;
    private final ObservabilityProperties observabilityProperties;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final Deque<LlmCallLog> localLlmLogs = new ArrayDeque<>();
    private final Deque<AuditEvent> localAuditEvents = new ArrayDeque<>();

    @Autowired
    public ObservabilityRepository(
            PersistenceProperties persistenceProperties,
            ObservabilityProperties observabilityProperties,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        this.persistenceProperties = persistenceProperties;
        this.observabilityProperties = observabilityProperties;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public synchronized void saveLlmCall(LlmCallLog item) {
        addBounded(localLlmLogs, item);
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!persistenceProperties.isEnabled() || jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO llm_call_log (
                                id, run_id, provider, model_name, task_type, success, latency_ms,
                                prompt_chars, response_chars, prompt_preview, response_preview,
                                error_message, started_at, finished_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    item.id(),
                    item.runId(),
                    item.provider(),
                    item.modelName(),
                    item.taskType(),
                    item.success(),
                    item.latencyMs(),
                    item.promptChars(),
                    item.responseChars(),
                    item.promptPreview(),
                    item.responsePreview(),
                    item.errorMessage(),
                    Timestamp.from(item.startedAt()),
                    Timestamp.from(item.finishedAt())
            );
        } catch (Exception exception) {
            log.debug("LLM 调用日志写入数据库失败，已保留在本地缓存：{}", exception.getMessage());
        }
    }

    public synchronized void saveAuditEvent(AuditEvent item) {
        addBounded(localAuditEvents, item);
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (!persistenceProperties.isEnabled() || jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO audit_event (
                                id, event_type, actor, run_id, target_type, target_id, detail, created_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    item.id(),
                    item.eventType(),
                    item.actor(),
                    item.runId(),
                    item.targetType(),
                    item.targetId(),
                    item.detail(),
                    Timestamp.from(item.createdAt())
            );
        } catch (Exception exception) {
            log.debug("审计事件写入数据库失败，已保留在本地缓存：{}", exception.getMessage());
        }
    }

    public List<LlmCallLog> latestLlmCalls(int limit) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (persistenceProperties.isEnabled() && jdbcTemplate != null) {
            try {
                return jdbcTemplate.query("""
                                SELECT id, run_id, provider, model_name, task_type, success, latency_ms,
                                       prompt_chars, response_chars, prompt_preview, response_preview,
                                       error_message, started_at, finished_at
                                FROM llm_call_log
                                ORDER BY started_at DESC
                                LIMIT ?
                                """,
                        (rs, rowNum) -> new LlmCallLog(
                                rs.getString("id"),
                                rs.getString("run_id"),
                                rs.getString("provider"),
                                rs.getString("model_name"),
                                rs.getString("task_type"),
                                rs.getBoolean("success"),
                                rs.getLong("latency_ms"),
                                rs.getInt("prompt_chars"),
                                rs.getInt("response_chars"),
                                rs.getString("prompt_preview"),
                                rs.getString("response_preview"),
                                rs.getString("error_message"),
                                rs.getTimestamp("started_at").toInstant(),
                                rs.getTimestamp("finished_at").toInstant()
                        ),
                        limit
                );
            } catch (Exception exception) {
                log.debug("LLM 调用日志读取数据库失败，改用本地缓存：{}", exception.getMessage());
            }
        }
        return localLlmLogs.stream()
                .sorted(Comparator.comparing(LlmCallLog::startedAt).reversed())
                .limit(limit)
                .toList();
    }

    public List<AuditEvent> latestAuditEvents(int limit) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (persistenceProperties.isEnabled() && jdbcTemplate != null) {
            try {
                return jdbcTemplate.query("""
                                SELECT id, event_type, actor, run_id, target_type, target_id, detail, created_at
                                FROM audit_event
                                ORDER BY created_at DESC
                                LIMIT ?
                                """,
                        (rs, rowNum) -> new AuditEvent(
                                rs.getString("id"),
                                rs.getString("event_type"),
                                rs.getString("actor"),
                                rs.getString("run_id"),
                                rs.getString("target_type"),
                                rs.getString("target_id"),
                                rs.getString("detail"),
                                rs.getTimestamp("created_at").toInstant()
                        ),
                        limit
                );
            } catch (Exception exception) {
                log.debug("审计事件读取数据库失败，改用本地缓存：{}", exception.getMessage());
            }
        }
        return localAuditEvents.stream()
                .sorted(Comparator.comparing(AuditEvent::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    private <T> void addBounded(Deque<T> values, T item) {
        values.addFirst(item);
        int maxSize = Math.max(10, observabilityProperties.getLocalBufferSize());
        while (values.size() > maxSize) {
            values.removeLast();
        }
    }
}


