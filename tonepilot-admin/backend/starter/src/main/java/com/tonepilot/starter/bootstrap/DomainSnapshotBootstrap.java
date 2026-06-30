package com.tonepilot.starter.bootstrap;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.controller.admin.*;
import com.tonepilot.application.dto.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.starter.advice.*;
import com.tonepilot.starter.bootstrap.*;
import com.tonepilot.starter.config.*;
import com.tonepilot.starter.security.*;







import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.colorgrading.ColorAdjustment;
import com.tonepilot.domain.photo.Photo;
import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainSnapshotBootstrap {

    private final InMemoryTonePilotStore store;
    private final DomainSnapshotRepository snapshotRepository;

    @Autowired
    public DomainSnapshotBootstrap(
            InMemoryTonePilotStore store,
            DomainSnapshotRepository snapshotRepository
    ) {
        this.store = store;
        this.snapshotRepository = snapshotRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreIdWatermarks() {
        snapshotRepository.list("photo", Photo.class)
                .forEach(item -> {
                    store.photos.putIfAbsent(item.id(), item);
                    bumpPhotoId(item.id());
                });
        snapshotRepository.list("photo_analysis", PhotoAnalysis.class)
                .forEach(item -> {
                    store.analyses.putIfAbsent(item.id(), item);
                    bumpAnalysisId(item.id());
                });
        snapshotRepository.list("color_adjustment", ColorAdjustment.class)
                .forEach(item -> {
                    store.adjustments.putIfAbsent(item.id(), item);
                    bumpAdjustmentId(item.id());
                });
    }

    private void bumpPhotoId(Long id) {
        if (id != null) {
            store.photoIds.updateAndGet(current -> Math.max(current, id + 1));
        }
    }

    private void bumpAnalysisId(Long id) {
        if (id != null) {
            store.analysisIds.updateAndGet(current -> Math.max(current, id + 1));
        }
    }

    private void bumpAdjustmentId(Long id) {
        if (id != null) {
            store.adjustmentIds.updateAndGet(current -> Math.max(current, id + 1));
        }
    }
}


