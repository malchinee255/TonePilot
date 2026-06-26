package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.evaluation.BenchmarkEvaluationService;
import com.tonepilot.evaluation.BenchmarkReport;
import com.tonepilot.evaluation.BenchmarkRequest;
import com.tonepilot.service.EvaluationHarnessService;
import com.tonepilot.web.dto.EvaluationRequest;
import com.tonepilot.web.dto.EvaluationResult;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationHarnessService evaluationHarnessService;
    private final BenchmarkEvaluationService benchmarkEvaluationService;

    public EvaluationController(
            EvaluationHarnessService evaluationHarnessService,
            BenchmarkEvaluationService benchmarkEvaluationService
    ) {
        this.evaluationHarnessService = evaluationHarnessService;
        this.benchmarkEvaluationService = benchmarkEvaluationService;
    }

    @PostMapping("/check")
    public ApiResponse<EvaluationResult> check(@RequestBody EvaluationRequest request) {
        return ApiResponse.ok(evaluationHarnessService.check(request));
    }

    @PostMapping("/benchmark")
    public ApiResponse<BenchmarkReport> benchmark(@RequestBody(required = false) BenchmarkRequest request) {
        return ApiResponse.ok(benchmarkEvaluationService.run(request));
    }
}
