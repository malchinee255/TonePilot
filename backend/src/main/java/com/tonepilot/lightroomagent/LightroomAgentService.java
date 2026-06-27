package com.tonepilot.lightroomagent;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LightroomAgentService {

    private final TuningAdjustmentPlanner tuningAdjustmentPlanner;
    private final LightroomDevelopSettingsMapper developSettingsMapper;

    public LightroomAgentService(
            TuningAdjustmentPlanner tuningAdjustmentPlanner,
            LightroomDevelopSettingsMapper developSettingsMapper
    ) {
        this.tuningAdjustmentPlanner = tuningAdjustmentPlanner;
        this.developSettingsMapper = developSettingsMapper;
    }

    public LightroomAgentTuneResponse tune(LightroomAgentTuneRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();
        ColorAdjustment current = request.currentAdjustment() == null
                ? neutralAdjustment(request.localPhotoId())
                : request.currentAdjustment();
        TuningPlan plan = tuningAdjustmentPlanner.apply(current, request.message());
        return new LightroomAgentTuneResponse(
                sessionId,
                request.localPhotoId(),
                plan.adjustment(),
                plan.deltas(),
                plan.assistantMessage(),
                developSettingsMapper.toDevelopSettings(plan.adjustment())
        );
    }

    private ColorAdjustment neutralAdjustment(String localPhotoId) {
        return new ColorAdjustment(
                null,
                null,
                "Lightroom 插件调色",
                "从 Lightroom 当前照片发起的 Agent 调色会话：" + (localPhotoId == null ? "unknown" : localPhotoId),
                new LightroomBasicParams(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                new LightroomHslParams(
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0,
                        0, 0, 0
                ),
                new LightroomEffectsParams(0, 0),
                List.of("读取 Lightroom 当前参数", "根据用户指令生成参数增量", "返回 Develop Settings 给插件应用"),
                Map.of("source", "lightroom-plugin"),
                Instant.now()
        );
    }
}
