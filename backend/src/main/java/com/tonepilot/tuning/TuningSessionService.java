package com.tonepilot.tuning;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.LightroomBasicParams;
import com.tonepilot.domain.LightroomEffectsParams;
import com.tonepilot.domain.LightroomHslParams;
import com.tonepilot.domain.Photo;
import com.tonepilot.observability.ObservabilityService;
import com.tonepilot.persistence.DomainSnapshotRepository;
import com.tonepilot.render.ImagePreviewRenderer;
import com.tonepilot.render.PreviewRender;
import com.tonepilot.service.PhotoService;
import com.tonepilot.store.InMemoryTonePilotStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TuningSessionService {

    private final InMemoryTonePilotStore store;
    private final PhotoService photoService;
    private final TuningAdjustmentPlanner planner;
    private final ImagePreviewRenderer previewRenderer;
    private final DomainSnapshotRepository snapshotRepository;
    private final ObservabilityService observabilityService;
    private final Map<String, TuningSession> sessions = new ConcurrentHashMap<>();

    public TuningSessionService(
            InMemoryTonePilotStore store,
            PhotoService photoService,
            TuningAdjustmentPlanner planner,
            ImagePreviewRenderer previewRenderer,
            DomainSnapshotRepository snapshotRepository,
            ObservabilityService observabilityService
    ) {
        this.store = store;
        this.photoService = photoService;
        this.planner = planner;
        this.previewRenderer = previewRenderer;
        this.snapshotRepository = snapshotRepository;
        this.observabilityService = observabilityService;
    }

    public TuningSession start(TuningStartRequest request) {
        if (request.photoId() == null) {
            throw new IllegalArgumentException("photoId 不能为空");
        }
        Photo photo = photoService.get(request.photoId());
        ColorAdjustment adjustment = resolveInitialAdjustment(request);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        PreviewRender preview = previewRenderer.render(photo, adjustment, sessionId, 0);
        Instant now = Instant.now();
        TuningSession session = new TuningSession(
                sessionId,
                photo.id(),
                adjustment.id(),
                adjustment,
                List.of(new TuningMessage("assistant", "已创建多轮调色会话，可以继续描述想要微调的方向。", now)),
                List.of(),
                preview,
                false,
                now,
                now
        );
        return saveSession(session);
    }

    public TuningSession sendMessage(String sessionId, TuningMessageRequest request) {
        TuningSession session = get(sessionId);
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("微调指令不能为空");
        }
        Photo photo = photoService.get(session.photoId());
        TuningPlan plan = planner.apply(session.currentAdjustment(), request.message());
        List<TuningMessage> messages = new ArrayList<>(session.messages());
        Instant now = Instant.now();
        messages.add(new TuningMessage("user", request.message(), now));
        messages.add(new TuningMessage("assistant", plan.assistantMessage(), now));
        PreviewRender preview = previewRenderer.render(photo, plan.adjustment(), session.id(), messages.size());
        TuningSession updated = new TuningSession(
                session.id(),
                session.photoId(),
                session.sourceAdjustmentId(),
                plan.adjustment(),
                List.copyOf(messages),
                plan.deltas(),
                preview,
                false,
                session.createdAt(),
                now
        );
        observabilityService.recordAuditEvent(
                "tuning.message_applied",
                "user",
                session.id(),
                "photo",
                String.valueOf(session.photoId()),
                "多轮微调，参数变化数=" + plan.deltas().size()
        );
        return saveSession(updated);
    }

    public TuningSession saveCurrent(String sessionId, TuningSaveRequest request) {
        TuningSession session = get(sessionId);
        String name = request == null || request.name() == null || request.name().isBlank()
                ? session.currentAdjustment().style() + " 微调版"
                : request.name().trim();
        ColorAdjustment current = session.currentAdjustment();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", current.rawResponse());
        metadata.put("tuningSessionId", session.id());
        metadata.put("sourceAdjustmentId", session.sourceAdjustmentId());
        metadata.put("previewUrl", session.preview() == null ? null : session.preview().previewUrl());
        metadata.put("latestDeltas", session.latestDeltas());
        ColorAdjustment saved = new ColorAdjustment(
                store.adjustmentIds.getAndIncrement(),
                session.photoId(),
                name,
                current.reason() + "；已通过多轮对话微调保存。",
                current.basic(),
                current.hsl(),
                current.effects(),
                appendStep(current.steps(), "保存多轮微调结果：" + name),
                metadata,
                Instant.now()
        );
        store.adjustments.put(saved.id(), saved);
        snapshotRepository.save("color_adjustment", saved.id(), saved);
        TuningSession updated = new TuningSession(
                session.id(),
                session.photoId(),
                session.sourceAdjustmentId(),
                saved,
                session.messages(),
                session.latestDeltas(),
                session.preview(),
                true,
                session.createdAt(),
                Instant.now()
        );
        observabilityService.recordAuditEvent(
                "tuning.saved",
                "user",
                session.id(),
                "adjustment",
                String.valueOf(saved.id()),
                "保存多轮微调结果，style=" + saved.style()
        );
        return saveSession(updated);
    }

    public TuningSession get(String sessionId) {
        TuningSession cached = sessions.get(sessionId);
        if (cached != null) {
            return cached;
        }
        return snapshotRepository.find("tuning_session", sessionId, TuningSession.class)
                .map(session -> {
                    sessions.put(session.id(), session);
                    return session;
                })
                .orElseThrow(() -> new NotFoundException("未找到调色会话：" + sessionId));
    }

    private TuningSession saveSession(TuningSession session) {
        sessions.put(session.id(), session);
        snapshotRepository.save("tuning_session", session.id(), session);
        return session;
    }

    private ColorAdjustment resolveInitialAdjustment(TuningStartRequest request) {
        if (request.adjustmentId() != null) {
            return findAdjustment(request.adjustmentId())
                    .orElseThrow(() -> new NotFoundException("未找到调色参数：" + request.adjustmentId()));
        }
        return latestAdjustmentForPhoto(request.photoId()).orElseGet(() -> neutralAdjustment(request.photoId()));
    }

    private Optional<ColorAdjustment> findAdjustment(Long adjustmentId) {
        ColorAdjustment cached = store.adjustments.get(adjustmentId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return snapshotRepository.find("color_adjustment", adjustmentId, ColorAdjustment.class)
                .map(adjustment -> {
                    store.adjustments.put(adjustment.id(), adjustment);
                    return adjustment;
                });
    }

    private Optional<ColorAdjustment> latestAdjustmentForPhoto(Long photoId) {
        snapshotRepository.list("color_adjustment", ColorAdjustment.class).stream()
                .filter(item -> item.photoId().equals(photoId))
                .forEach(item -> store.adjustments.putIfAbsent(item.id(), item));
        return store.latestAdjustmentForPhoto(photoId);
    }

    private ColorAdjustment neutralAdjustment(Long photoId) {
        return new ColorAdjustment(
                null,
                photoId,
                "中性起点",
                "没有找到已有调色参数，从中性 Lightroom 参数开始多轮微调。",
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
                List.of("从中性参数开始"),
                Map.of("source", "neutral"),
                Instant.now()
        );
    }

    private List<String> appendStep(List<String> source, String step) {
        List<String> steps = new ArrayList<>(source == null ? List.of() : source);
        steps.add(step);
        return List.copyOf(steps);
    }
}
