package com.tonepilot.rag;

import com.tonepilot.domain.KnowledgeChunk;
import com.tonepilot.web.dto.RagSearchItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRagServiceTest {

    @Test
    void ranksResultsWithKeywordVectorAndRerankScores() {
        HybridRagService service = new HybridRagService(new HybridRagProperties());
        List<KnowledgeChunk> chunks = List.of(
                new KnowledgeChunk(1L, "style_knowledge_chunk", 11L, "夜景电影感",
                        "夜景城市照片需要压高光，提阴影，保护灯光层次。",
                        Map.of("夜景", 0.9, "高光", 0.8, "电影感", 0.7), Instant.now()),
                new KnowledgeChunk(2L, "style_knowledge_chunk", 12L, "人像肤色",
                        "人像修图优先保护肤色，提升橙色明度。",
                        Map.of("人像", 0.9, "肤色", 0.8), Instant.now()),
                new KnowledgeChunk(3L, "style_knowledge_chunk", 13L, "夜景蓝调",
                        "城市夜景可以降低蓝色饱和度，但不要过度压暗。",
                        Map.of("夜景", 0.7, "蓝色", 0.8), Instant.now())
        );

        List<RagSearchItem> results = service.retrieve("夜景 电影感 压高光", chunks, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).title()).isEqualTo("夜景电影感");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }


    @Test
    void returnsEmptyResultsForBlankQueryOrMissingChunks() {
        HybridRagService service = new HybridRagService(new HybridRagProperties());

        assertThat(service.retrieve("   ", List.of(sampleChunk("night cinematic", "lower highlight")), 5)).isEmpty();
        assertThat(service.retrieve("night", List.of(), 5)).isEmpty();
        assertThat(service.retrieve("night", null, 5)).isEmpty();
    }

    @Test
    void respectsTopKMinimumAndKeepsPositiveMatchesOnly() {
        HybridRagService service = new HybridRagService(new HybridRagProperties());
        List<KnowledgeChunk> chunks = List.of(
                sampleChunk("night cinematic", "night city photo should lower highlights and lift shadows"),
                sampleChunk("night blue tone", "night photo can reduce blue saturation"),
                sampleChunk("portrait skin", "portrait should protect skin tone")
        );

        List<RagSearchItem> results = service.retrieve("night highlight", chunks, 0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).contains("night");
    }

    @Test
    void vectorWeightCanPromoteSemanticMatchWhenKeywordIsWeak() {
        HybridRagProperties properties = new HybridRagProperties();
        properties.setKeywordWeight(0.0);
        properties.setVectorWeight(1.0);
        properties.setRerankWeight(0.0);
        HybridRagService service = new HybridRagService(properties);
        List<KnowledgeChunk> chunks = List.of(
                new KnowledgeChunk(21L, "style_knowledge_chunk", 21L, "weak keyword", "generic color note",
                        Map.of("cinematic", 1.0, "night", 1.0), Instant.now()),
                new KnowledgeChunk(22L, "style_knowledge_chunk", 22L, "unrelated", "night highlight",
                        Map.of("portrait", 1.0), Instant.now())
        );

        List<RagSearchItem> results = service.retrieve("night cinematic", chunks, 2);

        assertThat(results.get(0).title()).isEqualTo("weak keyword");
    }

    @Test
    void propertiesExposeMilvusAndEmbeddingDefaults() {
        HybridRagProperties properties = new HybridRagProperties();

        assertThat(properties.getEmbedding().getProvider()).isEqualTo("openai");
        assertThat(properties.getEmbedding().getDimensions()).isEqualTo(1536);
        assertThat(properties.getVectorStore().getMilvusUri()).isEqualTo("http://localhost:19530");
        assertThat(properties.getVectorStore().getCollection()).isEqualTo("tonepilot_knowledge");
    }

    private KnowledgeChunk sampleChunk(String title, String content) {
        return new KnowledgeChunk(99L, "style_knowledge_chunk", 99L, title, content,
                Map.of("night", 0.7, "highlight", 0.6, "cinematic", 0.5), Instant.now());
    }

}
