package com.tonepilot.infrastructure.shared.persistence;

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


import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.application.agent.workflow.TonePilotAgentContext;
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
    public final AtomicLong knowledgeSourceIds = new AtomicLong(1);
    public final AtomicLong knowledgeMaterialIds = new AtomicLong(1);
    public final AtomicLong knowledgeExtractionJobIds = new AtomicLong(1);
    public final AtomicLong knowledgeChunkIds = new AtomicLong(1);

    public final Map<Long, Photo> photos = new ConcurrentHashMap<>();
    public final Map<Long, PhotoAnalysis> analyses = new ConcurrentHashMap<>();
    public final Map<Long, ColorKnowledge> knowledge = new ConcurrentHashMap<>();
    public final Map<Long, ColorAdjustment> adjustments = new ConcurrentHashMap<>();
    public final Map<Long, ColorStyle> styles = new ConcurrentHashMap<>();
    public final Map<Long, StyleSample> samples = new ConcurrentHashMap<>();
    public final Map<Long, StyleKnowledge> styleKnowledge = new ConcurrentHashMap<>();
    public final Map<Long, KnowledgeSource> knowledgeSources = new ConcurrentHashMap<>();
    public final Map<Long, KnowledgeMaterial> knowledgeMaterials = new ConcurrentHashMap<>();
    public final Map<Long, KnowledgeExtractionJob> knowledgeExtractionJobs = new ConcurrentHashMap<>();
    public final Map<Long, KnowledgeChunk> knowledgeChunks = new ConcurrentHashMap<>();
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
