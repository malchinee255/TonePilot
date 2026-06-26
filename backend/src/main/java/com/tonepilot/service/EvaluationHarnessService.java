package com.tonepilot.service;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.harness.ParamRangeValidator;
import com.tonepilot.harness.SceneRuleEvaluator;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.EvaluationRequest;
import com.tonepilot.web.dto.EvaluationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationHarnessService {

    private final InMemoryTonePilotStore store;
    private final PhotoService photoService;
    private final ParamRangeValidator paramRangeValidator;
    private final SceneRuleEvaluator sceneRuleEvaluator = new SceneRuleEvaluator();

    public EvaluationHarnessService(
            InMemoryTonePilotStore store,
            PhotoService photoService,
            ParamRangeValidator paramRangeValidator
    ) {
        this.store = store;
        this.photoService = photoService;
        this.paramRangeValidator = paramRangeValidator;
    }

    public EvaluationResult check(EvaluationRequest request) {
        PhotoAnalysis analysis = resolveAnalysis(request);
        ColorAdjustment adjustment = resolveAdjustment(request);
        List<String> issues = new ArrayList<>();

        if (adjustment.reason() == null || adjustment.reason().isBlank()) {
            issues.add("缺少 reason");
        } else {
            issues.add("包含 reason");
        }

        if (adjustment.steps() == null || adjustment.steps().isEmpty()) {
            issues.add("缺少 steps");
        } else {
            issues.add("包含 steps");
        }

        issues.addAll(paramRangeValidator.validate(adjustment));
        issues.addAll(sceneRuleEvaluator.evaluate(analysis, adjustment));

        long failures = issues.stream().filter(this::isFailure).count();
        int score = (int) Math.max(0, 100 - failures * 12);
        return new EvaluationResult(failures == 0, score, issues);
    }

    private PhotoAnalysis resolveAnalysis(EvaluationRequest request) {
        if (request.analysis() != null) {
            return request.analysis();
        }
        if (request.photoId() == null) {
            throw new IllegalArgumentException("必须提供 photoId 或 analysis");
        }
        return photoService.latestAnalysisOrAnalyze(request.photoId());
    }

    private ColorAdjustment resolveAdjustment(EvaluationRequest request) {
        if (request.adjustment() != null) {
            return request.adjustment();
        }
        if (request.adjustmentId() == null) {
            throw new IllegalArgumentException("必须提供 adjustmentId 或 adjustment");
        }
        ColorAdjustment adjustment = store.adjustments.get(request.adjustmentId());
        if (adjustment == null) {
            throw new NotFoundException("未找到调色方案：" + request.adjustmentId());
        }
        return adjustment;
    }

    private boolean isFailure(String issue) {
        return issue.contains("超出") || issue.contains("缺少") || issue.contains("未降低") || issue.contains("未提升") || issue.contains("建议增加");
    }
}
