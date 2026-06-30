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
}
