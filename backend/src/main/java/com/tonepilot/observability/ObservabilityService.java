package com.tonepilot.observability;

import com.tonepilot.ai.AiProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ObservabilityService {

    private final ObservabilityRepository repository;
    private final AiProperties aiProperties;

    public ObservabilityService(ObservabilityRepository repository, AiProperties aiProperties) {
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
