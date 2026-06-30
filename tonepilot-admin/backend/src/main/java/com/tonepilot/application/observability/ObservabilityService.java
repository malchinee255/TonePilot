package com.tonepilot.application.observability;

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

import com.tonepilot.infrastructure.ai.AiProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ObservabilityService {

    private final ObservabilityLogRepository repository;
    private final AiProperties aiProperties;

    @Autowired
    public ObservabilityService(ObservabilityLogRepository repository, AiProperties aiProperties) {
        this.repository = repository;
        this.aiProperties = aiProperties;
    }

    public void recordLlmCall(
            String taskType,
            String modelName,
            String prompt,
            String response,
            Throwable error,
            Instant startedAt,
            Instant finishedAt
    ) {
        boolean success = error == null;
        repository.saveLlmCall(new LlmCallLog(
                UUID.randomUUID().toString(),
                TraceContext.currentRunId(),
                aiProperties.activeProvider(),
                modelName,
                taskType,
                success,
                Duration.between(startedAt, finishedAt).toMillis(),
                length(prompt),
                length(response),
                preview(prompt),
                preview(response),
                error == null ? null : error.getMessage(),
                startedAt,
                finishedAt
        ));
    }

    public void recordAuditEvent(
            String eventType,
            String actor,
            String runId,
            String targetType,
            String targetId,
            String detail
    ) {
        repository.saveAuditEvent(new AuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                actor == null || actor.isBlank() ? "system" : actor,
                runId,
                targetType,
                targetId,
                detail,
                Instant.now()
        ));
    }

    public List<LlmCallLog> latestLlmCalls(int limit) {
        return repository.latestLlmCalls(safeLimit(limit));
    }

    public List<AuditEvent> latestAuditEvents(int limit) {
        return repository.latestAuditEvents(safeLimit(limit));
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String preview(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}


