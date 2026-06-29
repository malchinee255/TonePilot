package com.tonepilot.evaluation;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.agent.ColorPlanningAgent;
import com.tonepilot.ai.AiProviderContext;
import com.tonepilot.colorgrading.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.harness.ParamRangeValidator;
import com.tonepilot.observability.ObservabilityService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BenchmarkEvaluationService {

    private final ColorPlanningAgent colorPlanningAgent;
    private final ParamRangeValidator paramRangeValidator;
    private final ObservabilityService observabilityService;

    @Autowired
    public BenchmarkEvaluationService(
            ColorPlanningAgent colorPlanningAgent,
            ParamRangeValidator paramRangeValidator,
            ObservabilityService observabilityService
    ) {
        this.colorPlanningAgent = colorPlanningAgent;
        this.paramRangeValidator = paramRangeValidator;
        this.observabilityService = observabilityService;
    }

    public BenchmarkReport run(BenchmarkRequest request) {
        String runId = UUID.randomUUID().toString();
        List<String> providers = request == null || request.providers() == null || request.providers().isEmpty()
                ? List.of("rule")
                : request.providers();
        List<BenchmarkProviderResult> results = providers.stream()
                .map(provider -> evaluateProvider(provider, cases()))
                .toList();
        observabilityService.recordAuditEvent(
                "evaluation.benchmark_run",
                "system",
                runId,
                "benchmark",
                "default",
                "执行自动评测，providers=" + providers
        );
        return new BenchmarkReport(runId, Instant.now(), results);
    }

    private BenchmarkProviderResult evaluateProvider(String provider, List<BenchmarkCase> cases) {
        List<BenchmarkCaseResult> caseResults = cases.stream()
                .map(item -> evaluateCase(provider, item))
                .toList();
        int passedCount = (int) caseResults.stream().filter(BenchmarkCaseResult::passed).count();
        double averageScore = caseResults.stream().mapToInt(BenchmarkCaseResult::score).average().orElse(0);
        return new BenchmarkProviderResult(
                provider,
                caseResults.size(),
                passedCount,
                caseResults.isEmpty() ? 0 : (double) passedCount / caseResults.size(),
                Math.round(averageScore * 10.0) / 10.0,
                caseResults
        );
    }

    private BenchmarkCaseResult evaluateCase(String provider, BenchmarkCase item) {
        try {
            ColorAdjustment adjustment = AiProviderContext.use(
                    provider,
                    () -> colorPlanningAgent.plan(-1L, item.targetStyle(), item.analysis(), List.of())
            );
            List<String> issues = new ArrayList<>(paramRangeValidator.validate(adjustment));
            if (adjustment.reason() == null || adjustment.reason().isBlank()) {
                issues.add("缺少 reason");
            }
            if (adjustment.steps() == null || adjustment.steps().isEmpty()) {
                issues.add("缺少 steps");
            }
            long failures = issues.stream().filter(this::isFailure).count();
            int score = (int) Math.max(0, 100 - failures * 12);
            return new BenchmarkCaseResult(
                    item.caseId(),
                    item.name(),
                    item.targetStyle(),
                    failures == 0,
                    score,
                    adjustment.style(),
                    issues
            );
        } catch (Exception exception) {
            return new BenchmarkCaseResult(
                    item.caseId(),
                    item.name(),
                    item.targetStyle(),
                    false,
                    0,
                    null,
                    List.of("评测异常：" + exception.getMessage())
            );
        }
    }

    private List<BenchmarkCase> cases() {
        return List.of(
                new BenchmarkCase(
                        "portrait-soft-light",
                        "柔和人像",
                        "自然通透",
                        new PhotoAnalysis(
                                null,
                                -1L,
                                "室内人像",
                                "人物面部",
                                List.of("肤色略暗", "高光需要保留"),
                                List.of("色温略偏冷"),
                                List.of("背景颜色偏杂"),
                                List.of("自然通透", "柔和人像"),
                                "适合保守提亮、保护肤色并轻微降低背景饱和。",
                                Map.of("source", "benchmark"),
                                Instant.now()
                        )
                ),
                new BenchmarkCase(
                        "night-cinematic",
                        "夜景电影感",
                        "夜景电影感",
                        new PhotoAnalysis(
                                null,
                                -2L,
                                "城市夜景",
                                "街道与灯光",
                                List.of("暗部偏深", "高光灯牌容易溢出"),
                                List.of("混合光源"),
                                List.of("蓝色和黄色对比明显"),
                                List.of("夜景电影感", "低饱和纪实"),
                                "适合压高光、提暗部层次、轻微冷暖分离。",
                                Map.of("source", "benchmark"),
                                Instant.now()
                        )
                ),
                new BenchmarkCase(
                        "landscape-clean",
                        "清透风景",
                        "清透风景",
                        new PhotoAnalysis(
                                null,
                                -3L,
                                "自然风景",
                                "天空与植被",
                                List.of("整体反差偏低"),
                                List.of("白平衡基本正常"),
                                List.of("绿色略脏", "天空蓝色不够通透"),
                                List.of("清透风景", "自然通透"),
                                "适合增加微对比、控制绿色饱和并增强天空通透度。",
                                Map.of("source", "benchmark"),
                                Instant.now()
                        )
                )
        );
    }

    private boolean isFailure(String issue) {
        return issue.contains("超出") || issue.contains("缺少") || issue.contains("异常");
    }
}


