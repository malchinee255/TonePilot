package com.tonepilot.web;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.ColorKnowledge;
import com.tonepilot.service.KnowledgeService;
import com.tonepilot.web.dto.KnowledgeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Autowired
    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping
    public ApiResponse<ColorKnowledge> create(@Valid @RequestBody KnowledgeRequest request) {
        return ApiResponse.ok(knowledgeService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ColorKnowledge>> list() {
        return ApiResponse.ok(knowledgeService.list());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResponse.ok();
    }
}


