package com.tonepilot.web.admin;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.KnowledgeExtractionJob;
import com.tonepilot.domain.KnowledgeMaterial;
import com.tonepilot.domain.KnowledgeSource;
import com.tonepilot.service.KnowledgeMaterialIngestionService;
import com.tonepilot.web.dto.KnowledgeMaterialRequest;
import com.tonepilot.web.dto.KnowledgeSourceRequest;
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
