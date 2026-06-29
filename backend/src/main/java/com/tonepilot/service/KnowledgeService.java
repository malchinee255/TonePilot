package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorKnowledge;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.KnowledgeRequest;
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


