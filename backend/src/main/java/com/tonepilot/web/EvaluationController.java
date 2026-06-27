package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.evaluation.BenchmarkEvaluationService;
import com.tonepilot.evaluation.BenchmarkReport;
import com.tonepilot.evaluation.BenchmarkRequest;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final BenchmarkEvaluationService benchmarkEvaluationService;

    public EvaluationController(BenchmarkEvaluationService benchmarkEvaluationService) {
        this.benchmarkEvaluationService = benchmarkEvaluationService;
    }

    @PostMapping("/benchmark")
    public ApiResponse<BenchmarkReport> benchmark(@RequestBody(required = false) BenchmarkRequest request) {
        return ApiResponse.ok(benchmarkEvaluationService.run(request));
    }
}
