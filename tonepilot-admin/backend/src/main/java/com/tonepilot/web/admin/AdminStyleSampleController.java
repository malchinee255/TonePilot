package com.tonepilot.web.admin;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.domain.StyleSample;
import com.tonepilot.service.StyleKnowledgeService;
import com.tonepilot.service.StyleSampleService;
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


