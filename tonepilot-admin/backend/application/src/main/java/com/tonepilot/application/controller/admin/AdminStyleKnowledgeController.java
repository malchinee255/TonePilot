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
import com.tonepilot.application.knowledge.StyleKnowledgeService;
import com.tonepilot.application.dto.StyleKnowledgeRequest;
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


