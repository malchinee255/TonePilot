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


import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.application.style.StyleService;
import com.tonepilot.server.dto.StyleRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/admin/styles")
public class AdminStyleController {

    private final StyleService styleService;

    @Autowired
    public AdminStyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @PostMapping
    public ApiResponse<ColorStyle> create(@Valid @RequestBody StyleRequest request) {
        return ApiResponse.ok(styleService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ColorStyle>> list() {
        return ApiResponse.ok(styleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ColorStyle> get(@PathVariable Long id) {
        return ApiResponse.ok(styleService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ColorStyle> update(@PathVariable Long id, @Valid @RequestBody StyleRequest request) {
        return ApiResponse.ok(styleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        styleService.delete(id);
        return ApiResponse.ok();
    }
}


