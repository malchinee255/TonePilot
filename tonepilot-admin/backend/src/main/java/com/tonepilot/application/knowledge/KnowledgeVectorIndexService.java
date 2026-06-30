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


import com.tonepilot.domain.knowledge.KnowledgeChunk;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KnowledgeVectorIndexService {

    @Autowired
    private InMemoryTonePilotStore store;

    public List<KnowledgeChunk> indexStyleKnowledge(StyleKnowledge knowledge) {
        store.knowledgeChunks.values().removeIf(chunk ->
                "style_knowledge_chunk".equals(chunk.sourceType()) && chunk.sourceId().equals(knowledge.id())
        );
        List<String> chunks = splitChunks(knowledge);
        List<KnowledgeChunk> indexed = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk(
                    store.knowledgeChunkIds.getAndIncrement(),
                    "style_knowledge_chunk",
                    knowledge.id(),
                    knowledge.title() + " #" + (i + 1),
                    chunks.get(i),
                    embed(chunks.get(i)),
                    Instant.now()
            );
            store.knowledgeChunks.put(chunk.id(), chunk);
            indexed.add(chunk);
        }
        return indexed;
    }

    public Map<String, Double> embed(String text) {
        Map<String, Double> vector = new LinkedHashMap<>();
        for (String term : terms(text)) {
            vector.merge(term, 1.0, Double::sum);
        }
        double norm = Math.sqrt(vector.values().stream().mapToDouble(value -> value * value).sum());
        if (norm > 0) {
            vector.replaceAll((key, value) -> value / norm);
        }
        return vector;
    }

    public double cosine(Map<String, Double> left, Map<String, Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        return left.entrySet()
                .stream()
                .mapToDouble(entry -> entry.getValue() * right.getOrDefault(entry.getKey(), 0.0))
                .sum();
    }

    private List<String> splitChunks(StyleKnowledge knowledge) {
        String text = String.join("\n",
                knowledge.title(),
                knowledge.scene(),
                knowledge.targetStyle(),
                String.join("，", knowledge.problems()),
                String.join("，", knowledge.strategy()),
                knowledge.content()
        );
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n|\\n");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (current.length() + paragraph.length() > 500 && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(paragraph.trim()).append('\n');
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks.isEmpty() ? List.of(text.trim()) : chunks;
    }

    private List<String> terms(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[，。；;、,：:（）()\\[\\]{}]", " ");
        List<String> values = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            if (!part.isBlank()) {
                values.add(part);
            }
        }
        for (String keyword : List.of("夜景", "人像", "风光", "电影感", "胶片", "高光", "阴影", "白平衡", "色温", "饱和度", "蓝色", "绿色", "暗角", "蒙版")) {
            if (normalized.contains(keyword)) {
                values.add(keyword);
            }
        }
        return values;
    }
}
