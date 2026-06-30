package com.tonepilot.service;

import com.tonepilot.domain.KnowledgeExtractionJob;
import com.tonepilot.domain.KnowledgeMaterial;
import com.tonepilot.domain.KnowledgeSource;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.web.dto.KnowledgeMaterialRequest;
import com.tonepilot.web.dto.KnowledgeSourceRequest;
import com.tonepilot.web.dto.RagSearchItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "tonepilot.persistence.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class RagServiceTest {

    @Autowired
    private KnowledgeMaterialIngestionService ingestionService;

    @Autowired
    private StyleKnowledgeService styleKnowledgeService;

    @Autowired
    private RagService ragService;

    @Test
    void retrievesApprovedKnowledgeChunks() {
        KnowledgeSource source = ingestionService.createSource(new KnowledgeSourceRequest(
                "douyin_video",
                "夜景电影感教程",
                "调色博主",
                "https://www.douyin.com/video/vector",
                1L,
                ""
        ));
        KnowledgeMaterial material = ingestionService.importMaterial(source.id(), new KnowledgeMaterialRequest(
                "transcript",
                "字幕",
                "夜景城市照片要压高光，提升阴影，保护灯光层次，蓝色饱和度不要太高。",
                "zh-CN"
        ));
        KnowledgeExtractionJob job = ingestionService.extractToKnowledge(source.id(), material.id());
        StyleKnowledge approved = styleKnowledgeService.approve(job.generatedKnowledgeId());

        List<RagSearchItem> results = ragService.retrieve("夜景 高光 阴影", 3);

        assertThat(approved.status()).isEqualTo("approved");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).sourceType()).isEqualTo("style_knowledge_chunk");
        assertThat(results.get(0).title()).contains("夜景电影感教程");
        assertThat(results.get(0).content()).contains("压高光");
    }
}
