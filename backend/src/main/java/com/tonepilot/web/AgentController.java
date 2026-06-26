package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.service.ColorAgentService;
import com.tonepilot.web.dto.AdjustmentGenerateRequest;
import com.tonepilot.workflow.WorkflowRunSnapshot;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final ColorAgentService colorAgentService;

    public AgentController(ColorAgentService colorAgentService) {
        this.colorAgentService = colorAgentService;
    }

    @PostMapping("/generate-adjustment")
    public ApiResponse<ColorAdjustment> generate(@Valid @RequestBody AdjustmentGenerateRequest request) {
        return ApiResponse.ok(colorAgentService.generate(request));
    }

    @GetMapping("/workflows/{runId}")
    public ApiResponse<WorkflowRunSnapshot> workflow(@PathVariable String runId) {
        return ApiResponse.ok(colorAgentService.getWorkflow(runId));
    }
}
