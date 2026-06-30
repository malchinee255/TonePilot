package com.tonepilot.application.knowledge;

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

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.knowledge.ColorKnowledge;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import com.tonepilot.server.dto.KnowledgeRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeService {

    private final InMemoryTonePilotStore store;

    @Autowired
    public KnowledgeService(InMemoryTonePilotStore store) {
        this.store = store;
    }

    public ColorKnowledge create(KnowledgeRequest request) {
        ColorKnowledge knowledge = new ColorKnowledge(
                store.knowledgeIds.getAndIncrement(),
                request.title(),
                request.scene(),
                safeList(request.problems()),
                request.targetStyle(),
                safeList(request.strategy()),
                request.paramRanges() == null ? java.util.Map.of() : request.paramRanges(),
                isBlank(request.content()) ? buildContent(request) : request.content(),
                "local-" + UUID.randomUUID(),
                Instant.now()
        );
        store.knowledge.put(knowledge.id(), knowledge);
        return knowledge;
    }

    public List<ColorKnowledge> list() {
        return store.knowledge.values()
                .stream()
                .sorted(Comparator.comparing(ColorKnowledge::createdAt).reversed())
                .toList();
    }

    public ColorKnowledge get(Long id) {
        ColorKnowledge knowledge = store.knowledge.get(id);
        if (knowledge == null) {
            throw new NotFoundException("未找到知识条目：" + id);
        }
        return knowledge;
    }

    public void delete(Long id) {
        if (store.knowledge.remove(id) == null) {
            throw new NotFoundException("未找到知识条目：" + id);
        }
    }

    private String buildContent(KnowledgeRequest request) {
        return "%s，场景：%s，目标风格：%s，问题：%s，策略：%s，参数范围：%s"
                .formatted(
                        request.title(),
                        request.scene(),
                        request.targetStyle(),
                        safeList(request.problems()),
                        safeList(request.strategy()),
                        request.paramRanges()
                );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


