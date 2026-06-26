package com.tonepilot.service;

import com.tonepilot.domain.ColorKnowledge;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.RagSearchItem;
import com.tonepilot.web.dto.RagSearchRequest;
import com.tonepilot.web.dto.RagSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RagService {

    private final InMemoryTonePilotStore store;
    private final int defaultTopK;

    public RagService(InMemoryTonePilotStore store, @Value("${tonepilot.rag.default-top-k:5}") int defaultTopK) {
        this.store = store;
        this.defaultTopK = defaultTopK;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        String query = normalizeQuery(request.query(), request.photoId());
        int topK = request.topK() == null || request.topK() <= 0 ? defaultTopK : request.topK();
        return new RagSearchResponse(query, retrieve(query, topK));
    }

    public List<RagSearchItem> retrieve(String query, int topK) {
        List<RagSearchItem> items = new ArrayList<>();

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
                .limit(topK)
                .toList();
    }

    private String normalizeQuery(String query, Long photoId) {
        if (query != null && !query.isBlank()) {
            return query;
        }
        if (photoId != null) {
            return store.latestAnalysisForPhoto(photoId)
                    .map(this::queryFromAnalysis)
                    .orElse("");
        }
        return "";
    }

    private String queryFromAnalysis(PhotoAnalysis analysis) {
        return String.join("，",
                analysis.scene(),
                analysis.subject(),
                String.join("，", analysis.exposureIssues()),
                String.join("，", analysis.whiteBalanceIssues()),
                String.join("，", analysis.colorIssues()),
                String.join("，", analysis.recommendedStyles())
        );
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
