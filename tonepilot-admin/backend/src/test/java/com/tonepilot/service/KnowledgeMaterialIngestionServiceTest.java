package com.tonepilot.service;

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




import com.tonepilot.domain.knowledge.KnowledgeExtractionJob;
import com.tonepilot.domain.knowledge.KnowledgeMaterial;
import com.tonepilot.domain.knowledge.KnowledgeSource;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.server.dto.KnowledgeMaterialRequest;
import com.tonepilot.server.dto.KnowledgeSourceRequest;
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

    @Test
    void importsDouyinLinkAsIngestionJobAndCreatesPendingKnowledge() {
        KnowledgeExtractionJob job = service.importDouyinVideo(new com.tonepilot.server.dto.DouyinImportRequest(
                "https://www.douyin.com/video/123456",
                "城市夜景蓝橙电影感教程",
                "调色博主",
                1L,
                "教程提到压高光、提阴影、降低绿色饱和度。"
        ));

        assertThat(job.status()).isEqualTo("succeeded");
        assertThat(job.generatedKnowledgeId()).isNotNull();

        StyleKnowledge knowledge = styleKnowledgeService.get(job.generatedKnowledgeId());
        assertThat(knowledge.status()).isEqualTo("pending");
        assertThat(knowledge.scene()).isEqualTo("夜景");
        assertThat(knowledge.content()).contains("https://www.douyin.com/video/123456");
        assertThat(knowledge.content()).contains("教程提到压高光");
    }
}
