package com.tonepilot.store;

import com.tonepilot.domain.*;
import com.tonepilot.workflow.TonePilotAgentContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryTonePilotStore {

    public final AtomicLong photoIds = new AtomicLong(1);
    public final AtomicLong analysisIds = new AtomicLong(1);
    public final AtomicLong knowledgeIds = new AtomicLong(1);
    public final AtomicLong adjustmentIds = new AtomicLong(1);
    public final AtomicLong styleIds = new AtomicLong(1);
    public final AtomicLong sampleIds = new AtomicLong(1);
    public final AtomicLong styleKnowledgeIds = new AtomicLong(1);

    public final Map<Long, Photo> photos = new ConcurrentHashMap<>();
    public final Map<Long, PhotoAnalysis> analyses = new ConcurrentHashMap<>();
    public final Map<Long, ColorKnowledge> knowledge = new ConcurrentHashMap<>();
    public final Map<Long, ColorAdjustment> adjustments = new ConcurrentHashMap<>();
    public final Map<Long, ColorStyle> styles = new ConcurrentHashMap<>();
    public final Map<Long, StyleSample> samples = new ConcurrentHashMap<>();
    public final Map<Long, StyleKnowledge> styleKnowledge = new ConcurrentHashMap<>();
    public final Map<String, TonePilotAgentContext> workflowRuns = new ConcurrentHashMap<>();

    public Optional<PhotoAnalysis> latestAnalysisForPhoto(Long photoId) {
        return analyses.values()
                .stream()
                .filter(item -> item.photoId().equals(photoId))
                .max(Comparator.comparing(PhotoAnalysis::createdAt));
    }

    public Optional<ColorAdjustment> latestAdjustmentForPhoto(Long photoId) {
        return adjustments.values()
                .stream()
                .filter(item -> item.photoId().equals(photoId))
                .max(Comparator.comparing(ColorAdjustment::createdAt));
    }
}
