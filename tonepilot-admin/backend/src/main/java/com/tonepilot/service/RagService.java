package com.tonepilot.service;

import com.tonepilot.domain.ColorKnowledge;
import com.tonepilot.domain.KnowledgeChunk;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.rag.HybridRagService;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.RagSearchItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {

    @Autowired
    private InMemoryTonePilotStore store;

    @Autowired
    private KnowledgeVectorIndexService vectorIndexService;

    @Autowired
    private HybridRagService hybridRagService;

    @Value("${tonepilot.rag.default-top-k:5}")
    private int defaultTopK;

    public List<RagSearchItem> retrieve(String query, int topK) {
        List<KnowledgeChunk> chunks = new ArrayList<>(store.knowledgeChunks.values());
        chunks.addAll(colorKnowledgeChunks());
        chunks.addAll(styleKnowledgeChunks());
        return hybridRagService.retrieve(query, chunks, topK > 0 ? topK : defaultTopK);
    }

    private List<KnowledgeChunk> colorKnowledgeChunks() {
        return store.knowledge.values()
                .stream()
                .map(knowledge -> {
                    String corpus = String.join(" ", knowledge.title(), knowledge.scene(), knowledge.targetStyle(), knowledge.content());
                    return new KnowledgeChunk(
                            knowledge.id(),
                            "color_knowledge",
                            knowledge.id(),
                            knowledge.title(),
                            corpus,
                            vectorIndexService.embed(corpus),
                            Instant.now()
                    );
                })
                .toList();
    }

    private List<KnowledgeChunk> styleKnowledgeChunks() {
        return store.styleKnowledge.values()
                .stream()
                .filter(knowledge -> "approved".equals(knowledge.status()))
                .map(this::styleKnowledgeChunk)
                .toList();
    }

    private KnowledgeChunk styleKnowledgeChunk(StyleKnowledge knowledge) {
        String corpus = String.join(" ", knowledge.title(), knowledge.scene(), knowledge.targetStyle(), knowledge.content());
        return new KnowledgeChunk(
                knowledge.id(),
                "style_knowledge_chunk",
                knowledge.id(),
                knowledge.title(),
                corpus,
                vectorIndexService.embed(corpus),
                Instant.now()
        );
    }
}
