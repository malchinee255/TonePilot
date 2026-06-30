package com.tonepilot.service;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.controller.admin.*;
import com.tonepilot.application.dto.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.starter.advice.*;
import com.tonepilot.starter.bootstrap.*;
import com.tonepilot.starter.config.*;
import com.tonepilot.starter.security.*;



import com.tonepilot.starter.TonePilotApplication;






import com.tonepilot.domain.knowledge.KnowledgeExtractionJob;
import com.tonepilot.domain.knowledge.KnowledgeMaterial;
import com.tonepilot.domain.knowledge.KnowledgeSource;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.application.dto.KnowledgeMaterialRequest;
import com.tonepilot.application.dto.KnowledgeSourceRequest;
import com.tonepilot.domain.knowledge.RagSearchItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TonePilotApplication.class, properties = {
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
