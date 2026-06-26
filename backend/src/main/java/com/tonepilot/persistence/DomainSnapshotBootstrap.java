package com.tonepilot.persistence;

import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.store.InMemoryTonePilotStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainSnapshotBootstrap {

    private final InMemoryTonePilotStore store;
    private final DomainSnapshotRepository snapshotRepository;

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
