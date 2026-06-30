package com.tonepilot.application.controller.admin;

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







import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.application.dto.ApiResponse;
import com.tonepilot.domain.knowledge.StyleKnowledge;
import com.tonepilot.domain.style.StyleSample;
import com.tonepilot.application.knowledge.StyleKnowledgeService;
import com.tonepilot.application.style.StyleSampleService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/admin/style-samples")
public class AdminStyleSampleController {

    private final StyleSampleService styleSampleService;
    private final StyleKnowledgeService styleKnowledgeService;

    @Autowired
    public AdminStyleSampleController(
            StyleSampleService styleSampleService,
            StyleKnowledgeService styleKnowledgeService
    ) {
        this.styleSampleService = styleSampleService;
        this.styleKnowledgeService = styleKnowledgeService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StyleSample> upload(
            @RequestParam Long styleId,
            @RequestParam(defaultValue = "final_only") String sampleType,
            @RequestParam(defaultValue = "manual_upload") String sourceType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) MultipartFile finalImage,
            @RequestParam(required = false) MultipartFile beforeImage,
            @RequestParam(required = false) MultipartFile afterImage
    ) {
        return ApiResponse.ok(styleSampleService.upload(
                styleId,
                sampleType,
                sourceType,
                description,
                tags,
                finalImage,
                beforeImage,
                afterImage
        ));
    }

    @GetMapping
    public ApiResponse<List<StyleSample>> list(@RequestParam(required = false) Long styleId) {
        return ApiResponse.ok(styleSampleService.list(styleId));
    }

    @PostMapping("/{sampleId}/analyze")
    public ApiResponse<StyleSample> analyze(
            @PathVariable Long sampleId,
            @RequestParam(required = false) String provider
    ) {
        return ApiResponse.ok(styleSampleService.analyze(sampleId, provider));
    }

    @PostMapping("/{sampleId}/generate-knowledge")
    public ApiResponse<StyleKnowledge> generateKnowledge(
            @PathVariable Long sampleId,
            @RequestParam(required = false) String provider
    ) {
        return ApiResponse.ok(styleKnowledgeService.generateFromSample(sampleId, provider));
    }
}


