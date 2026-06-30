package com.tonepilot.application.photo;

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







import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.domain.agent.ImageAnalysisAgent;
import com.tonepilot.infrastructure.ai.AiProviderContext;
import com.tonepilot.domain.common.NotFoundException;
import com.tonepilot.domain.photo.Photo;
import com.tonepilot.domain.photo.PhotoAnalysis;
import com.tonepilot.infrastructure.shared.persistence.DomainSnapshotRepository;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class PhotoService {

    private final InMemoryTonePilotStore store;
    private final ObjectStorageService storageService;
    private final ImageAnalysisAgent imageAnalysisAgent;
    private final DomainSnapshotRepository snapshotRepository;

    @Autowired
    public PhotoService(
            InMemoryTonePilotStore store,
            ObjectStorageService storageService,
            ImageAnalysisAgent imageAnalysisAgent,
            DomainSnapshotRepository snapshotRepository
    ) {
        this.store = store;
        this.storageService = storageService;
        this.imageAnalysisAgent = imageAnalysisAgent;
        this.snapshotRepository = snapshotRepository;
    }

    public Photo upload(MultipartFile file) {
        StoredFile storedFile = storageService.storeImage(file, "photos");
        Photo photo = new Photo(
                store.photoIds.getAndIncrement(),
                storedFile.fileName(),
                storedFile.fileUrl(),
                storedFile.fileType(),
                Instant.now()
        );
        store.photos.put(photo.id(), photo);
        snapshotRepository.save("photo", photo.id(), photo);
        return photo;
    }

    public Photo get(Long id) {
        Photo cached = store.photos.get(id);
        if (cached != null) {
            return cached;
        }
        return snapshotRepository.find("photo", id, Photo.class)
                .map(photo -> {
                    store.photos.put(photo.id(), photo);
                    return photo;
                })
                .orElseThrow(() -> new NotFoundException("未找到照片：" + id));
    }

    public List<Photo> list() {
        snapshotRepository.list("photo", Photo.class).forEach(photo -> store.photos.putIfAbsent(photo.id(), photo));
        return store.photos.values().stream().sorted((a, b) -> b.uploadedAt().compareTo(a.uploadedAt())).toList();
    }

    public PhotoAnalysis analyze(Long photoId) {
        return analyze(photoId, null);
    }

    public PhotoAnalysis analyze(Long photoId, String provider) {
        Photo photo = get(photoId);
        PhotoAnalysis draft = AiProviderContext.use(provider, () -> imageAnalysisAgent.analyze(photo));
        PhotoAnalysis saved = new PhotoAnalysis(
                store.analysisIds.getAndIncrement(),
                photo.id(),
                draft.scene(),
                draft.subject(),
                draft.exposureIssues(),
                draft.whiteBalanceIssues(),
                draft.colorIssues(),
                draft.recommendedStyles(),
                draft.summary(),
                draft.rawResponse(),
                Instant.now()
        );
        store.analyses.put(saved.id(), saved);
        snapshotRepository.save("photo_analysis", saved.id(), saved);
        return saved;
    }

    public PhotoAnalysis latestAnalysisOrAnalyze(Long photoId) {
        return store.latestAnalysisForPhoto(photoId).orElseGet(() -> analyze(photoId));
    }

    public PhotoAnalysis latestAnalysisOrAnalyze(Long photoId, String provider) {
        return store.latestAnalysisForPhoto(photoId)
                .or(() -> latestPersistedAnalysisForPhoto(photoId))
                .orElseGet(() -> analyze(photoId, provider));
    }

    private java.util.Optional<PhotoAnalysis> latestPersistedAnalysisForPhoto(Long photoId) {
        snapshotRepository.list("photo_analysis", PhotoAnalysis.class)
                .stream()
                .filter(item -> item.photoId().equals(photoId))
                .forEach(item -> store.analyses.putIfAbsent(item.id(), item));
        return store.latestAnalysisForPhoto(photoId);
    }
}


