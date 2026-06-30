package com.tonepilot.infrastructure.knowledge.rag;

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
import com.tonepilot.server.dto.RagSearchItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HybridRagService {

    @Autowired
    private HybridRagProperties properties;

    public HybridRagService() {
    }

    public HybridRagService(HybridRagProperties properties) {
        this.properties = properties;
    }

    public List<RagSearchItem> retrieve(String query, List<KnowledgeChunk> chunks, int topK) {
        if (query == null || query.isBlank() || chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        Map<String, Double> queryVector = embed(query);
        List<String> queryTerms = terms(query);
        return chunks.stream()
                .map(chunk -> toItem(query, queryTerms, queryVector, chunk))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparing(RagSearchItem::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private RagSearchItem toItem(String query, List<String> queryTerms, Map<String, Double> queryVector, KnowledgeChunk chunk) {
        double keywordScore = keywordScore(query, queryTerms, chunk.content() + " " + chunk.title());
        double vectorScore = cosine(queryVector, chunk.embedding());
        double rerankScore = rerankScore(query, chunk);
        double score = keywordScore * properties.getKeywordWeight()
                + vectorScore * properties.getVectorWeight()
                + rerankScore * properties.getRerankWeight();
        return new RagSearchItem(chunk.sourceType(), chunk.sourceId(), chunk.title(), round(score), chunk.content());
    }

    private double keywordScore(String query, List<String> terms, String corpus) {
        if (terms.isEmpty() || corpus == null || corpus.isBlank()) {
            return 0;
        }
        String normalizedCorpus = corpus.toLowerCase(Locale.ROOT);
        long matches = terms.stream().filter(normalizedCorpus::contains).count();
        double coverage = (double) matches / terms.size();
        double exactBonus = normalizedCorpus.contains(query.toLowerCase(Locale.ROOT)) ? 0.15 : 0;
        return Math.min(1, coverage + exactBonus);
    }

    private double rerankScore(String query, KnowledgeChunk chunk) {
        String title = String.valueOf(chunk.title()).toLowerCase(Locale.ROOT);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        if (title.contains(normalizedQuery)) {
            return 1;
        }
        return terms(query).stream().anyMatch(title::contains) ? 0.6 : 0;
    }

    private Map<String, Double> embed(String text) {
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

    private double cosine(Map<String, Double> left, Map<String, Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        return left.entrySet()
                .stream()
                .mapToDouble(entry -> entry.getValue() * right.getOrDefault(entry.getKey(), 0.0))
                .sum();
    }

    private List<String> terms(String text) {
        String normalized = String.valueOf(text).toLowerCase(Locale.ROOT)
                .replaceAll("[，。；;、！？!?,.()\\[\\]{}]", " ");
        List<String> values = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            if (!part.isBlank()) {
                values.add(part);
            }
        }
        for (String keyword : List.of("夜景", "人像", "风光", "电影感", "胶片", "高光", "阴影", "白平衡", "色温", "饱和度", "蓝色", "绿色", "暗角", "蒙版")) {
            if (normalized.contains(keyword) && !values.contains(keyword)) {
                values.add(keyword);
            }
        }
        return values;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
