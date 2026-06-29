package com.tonepilot.web;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.observability.AuditEvent;
import com.tonepilot.observability.LlmCallLog;
import com.tonepilot.observability.ObservabilityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    @Autowired
    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/llm-calls")
    public ApiResponse<List<LlmCallLog>> llmCalls(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(observabilityService.latestLlmCalls(limit));
    }

    @GetMapping("/audit-events")
    public ApiResponse<List<AuditEvent>> auditEvents(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(observabilityService.latestAuditEvents(limit));
    }
}


