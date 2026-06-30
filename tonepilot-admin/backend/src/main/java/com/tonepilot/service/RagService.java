package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.ColorKnowledge;
import com.tonepilot.domain.KnowledgeChunk;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.RagSearchItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final InMemoryTonePilotStore store;
    private final int defaultTopK;
    private final KnowledgeVectorIndexService vectorIndexService;

    @Autowired
    public RagService(
            InMemoryTonePilotStore store,
            KnowledgeVectorIndexService vectorIndexService,
            @Value("${tonepilot.rag.default-top-k:5}") int defaultTopK
    ) {
        this.store = store;
        this.vectorIndexService = vectorIndexService;
        this.defaultTopK = defaultTopK;
    }

    public List<RagSearchItem> retrieve(String query, int topK) {
        List<RagSearchItem> items = new ArrayList<>();
        Map<String, Double> queryVector = vectorIndexService.embed(query);

        for (KnowledgeChunk chunk : store.knowledgeChunks.values()) {
            double score = Math.max(
                    score(query, chunk.content()),
                    vectorIndexService.cosine(queryVector, chunk.embedding())
            );
            if (score > 0) {
                items.add(new RagSearchItem(chunk.sourceType(), chunk.sourceId(), chunk.title(), score, chunk.content()));
            }
        }

        for (ColorKnowledge knowledge : store.knowledge.values()) {
            String corpus = String.join(" ", knowledge.title(), knowledge.scene(), knowledge.targetStyle(), knowledge.content());
            double score = score(query, corpus);
            if (score > 0) {
                items.add(new RagSearchItem("color_knowledge", knowledge.id(), knowledge.title(), score, knowledge.content()));
            }
        }

        for (StyleKnowledge knowledge : store.styleKnowledge.values()) {
            if (!"approved".equals(knowledge.status())) {
                continue;
            }
            String corpus = String.join(" ", knowledge.title(), knowledge.scene(), knowledge.targetStyle(), knowledge.content());
            double score = score(query, corpus);
            if (score > 0) {
                items.add(new RagSearchItem("style_knowledge", knowledge.id(), knowledge.title(), score, knowledge.content()));
            }
        }

        return items.stream()
                .sorted(Comparator.comparing(RagSearchItem::score).reversed())
                .limit(topK <= 0 ? defaultTopK : topK)
                .toList();
    }

    private double score(String query, String corpus) {
        if (query == null || query.isBlank() || corpus == null || corpus.isBlank()) {
            return 0;
        }

        List<String> terms = terms(query);
        if (terms.isEmpty()) {
            return 0;
        }

        String normalizedCorpus = corpus.toLowerCase();
        long matches = terms.stream().filter(normalizedCorpus::contains).count();
        double coverage = (double) matches / terms.size();
        double exactBonus = normalizedCorpus.contains(query.toLowerCase()) ? 0.2 : 0;
        return Math.min(1, coverage + exactBonus);
    }

    private List<String> terms(String query) {
        String[] parts = query.toLowerCase()
                .replaceAll("[，。；;、,]", " ")
                .split("\\s+");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                values.add(part);
            }
        }
        return values;
    }
}


