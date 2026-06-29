package com.tonepilot.web.admin;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.StyleKnowledge;
import com.tonepilot.service.StyleKnowledgeService;
import com.tonepilot.web.dto.StyleKnowledgeRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/admin/knowledge")
public class AdminStyleKnowledgeController {

    private final StyleKnowledgeService styleKnowledgeService;

    @Autowired
    public AdminStyleKnowledgeController(StyleKnowledgeService styleKnowledgeService) {
        this.styleKnowledgeService = styleKnowledgeService;
    }

    @GetMapping
    public ApiResponse<List<StyleKnowledge>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(styleKnowledgeService.list(status));
    }

    @PutMapping("/{id}")
    public ApiResponse<StyleKnowledge> update(@PathVariable Long id, @RequestBody StyleKnowledgeRequest request) {
        return ApiResponse.ok(styleKnowledgeService.update(id, request));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<StyleKnowledge> approve(@PathVariable Long id) {
        return ApiResponse.ok(styleKnowledgeService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<StyleKnowledge> reject(@PathVariable Long id) {
        return ApiResponse.ok(styleKnowledgeService.reject(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<StyleKnowledge> disable(@PathVariable Long id) {
        return ApiResponse.ok(styleKnowledgeService.disable(id));
    }
}


