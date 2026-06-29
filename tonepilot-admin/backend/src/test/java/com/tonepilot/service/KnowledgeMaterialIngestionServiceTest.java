package com.tonepilot.service;

import com.tonepilot.domain.KnowledgeExtractionJob;
import com.tonepilot.domain.KnowledgeMaterial;
import com.tonepilot.domain.KnowledgeSource;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.web.dto.KnowledgeMaterialRequest;
import com.tonepilot.web.dto.KnowledgeSourceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "tonepilot.persistence.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class KnowledgeMaterialIngestionServiceTest {

    @Autowired
    private KnowledgeMaterialIngestionService service;

    @Autowired
    private StyleKnowledgeService styleKnowledgeService;

    @Test
    void importsTextMaterialAndExtractsPendingStyleKnowledge() {
        KnowledgeSource source = service.createSource(new KnowledgeSourceRequest(
                "douyin_video",
                "夜景电影感调色教程",
                "调色博主",
                "https://www.douyin.com/video/example",
                1L,
                "用于沉淀夜景高光压制和暗部恢复经验"
        ));

        KnowledgeMaterial material = service.importMaterial(source.id(), new KnowledgeMaterialRequest(
                "transcript",
                "字幕摘要",
                "夜景照片先压高光，提一点阴影，绿色饱和度降低，整体做冷暖对比。",
                "zh-CN"
        ));

        KnowledgeExtractionJob job = service.extractToKnowledge(source.id(), material.id());

        assertThat(job.status()).isEqualTo("succeeded");
        assertThat(job.generatedKnowledgeId()).isNotNull();

        StyleKnowledge knowledge = styleKnowledgeService.get(job.generatedKnowledgeId());
        assertThat(knowledge.status()).isEqualTo("pending");
        assertThat(knowledge.title()).contains("夜景电影感调色教程");
        assertThat(knowledge.content()).contains("来源类型：douyin_video");
        assertThat(knowledge.content()).contains("夜景照片先压高光");
    }
}
