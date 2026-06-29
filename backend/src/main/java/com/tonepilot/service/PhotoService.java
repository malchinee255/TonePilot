package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.agent.ImageAnalysisAgent;
import com.tonepilot.ai.AiProviderContext;
import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.persistence.DomainSnapshotRepository;
import com.tonepilot.store.InMemoryTonePilotStore;
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


