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


import com.tonepilot.domain.knowledge.ColorKnowledge;
import com.tonepilot.domain.knowledge.KnowledgeChunk;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.infrastructure.knowledge.rag.HybridRagService;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import com.tonepilot.server.dto.RagSearchItem;
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
