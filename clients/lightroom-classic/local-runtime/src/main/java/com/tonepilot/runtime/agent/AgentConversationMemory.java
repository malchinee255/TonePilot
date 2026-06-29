package com.tonepilot.runtime.agent;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentConversationMemory {

    private final Map<String, PendingDecision> pendingDecisions = new ConcurrentHashMap<>();

    public void rememberDecision(String sessionId, PendingDecision decision) {
        pendingDecisions.put(sessionId, decision);
    }

    public Optional<PendingDecision> pendingDecision(String sessionId) {
        return Optional.ofNullable(pendingDecisions.get(sessionId));
    }

    public void clearPendingDecision(String sessionId) {
        pendingDecisions.remove(sessionId);
    }

    public record PendingDecision(
            String sessionId,
            String userIntent,
            AgentTuneResult tuneResult,
            Map<String, Object> photo,
            String beforePreviewUrl,
            Instant createdAt
    ) {
    }
}
