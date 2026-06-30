package com.tonepilot.server.admin;

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


import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.knowledge.KnowledgeExtractionJob;
import com.tonepilot.domain.knowledge.KnowledgeMaterial;
import com.tonepilot.domain.knowledge.KnowledgeSource;
import com.tonepilot.application.knowledge.KnowledgeMaterialIngestionService;
import com.tonepilot.server.dto.DouyinImportRequest;
import com.tonepilot.server.dto.KnowledgeMaterialRequest;
import com.tonepilot.server.dto.KnowledgeSourceRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/admin/knowledge-sources")
public class AdminKnowledgeMaterialController {

    @Autowired
    private KnowledgeMaterialIngestionService ingestionService;

    @GetMapping
    public ApiResponse<List<KnowledgeSource>> listSources() {
        return ApiResponse.ok(ingestionService.listSources());
    }

    @PostMapping
    public ApiResponse<KnowledgeSource> createSource(@Valid @RequestBody KnowledgeSourceRequest request) {
        return ApiResponse.ok(ingestionService.createSource(request));
    }

    @PostMapping("/douyin-imports")
    public ApiResponse<KnowledgeExtractionJob> importDouyinVideo(@Valid @RequestBody DouyinImportRequest request) {
        return ApiResponse.ok(ingestionService.importDouyinVideo(request));
    }

    @GetMapping("/{sourceId}/materials")
    public ApiResponse<List<KnowledgeMaterial>> listMaterials(@PathVariable Long sourceId) {
        return ApiResponse.ok(ingestionService.listMaterials(sourceId));
    }

    @PostMapping("/{sourceId}/materials")
    public ApiResponse<KnowledgeMaterial> importMaterial(
            @PathVariable Long sourceId,
            @Valid @RequestBody KnowledgeMaterialRequest request
    ) {
        return ApiResponse.ok(ingestionService.importMaterial(sourceId, request));
    }

    @PostMapping("/{sourceId}/materials/{materialId}/extract")
    public ApiResponse<KnowledgeExtractionJob> extractToKnowledge(
            @PathVariable Long sourceId,
            @PathVariable Long materialId
    ) {
        return ApiResponse.ok(ingestionService.extractToKnowledge(sourceId, materialId));
    }
}
