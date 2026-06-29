package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.agent.StyleAnalysisAgent;
import com.tonepilot.ai.AiProviderContext;
import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorStyle;
import com.tonepilot.domain.StyleAnalysisResult;
import com.tonepilot.domain.StyleSample;
import com.tonepilot.store.InMemoryTonePilotStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class StyleSampleService {

    private final InMemoryTonePilotStore store;
    private final StyleService styleService;
    private final ObjectStorageService storageService;
    private final StyleAnalysisAgent styleAnalysisAgent;

    @Autowired
    public StyleSampleService(
            InMemoryTonePilotStore store,
            StyleService styleService,
            ObjectStorageService storageService,
            StyleAnalysisAgent styleAnalysisAgent
    ) {
        this.store = store;
        this.styleService = styleService;
        this.storageService = storageService;
        this.styleAnalysisAgent = styleAnalysisAgent;
    }

    public StyleSample upload(
            Long styleId,
            String sampleType,
            String sourceType,
            String description,
            List<String> tags,
            MultipartFile finalImage,
            MultipartFile beforeImage,
            MultipartFile afterImage
    ) {
        styleService.get(styleId);
        String beforeUrl = beforeImage == null || beforeImage.isEmpty() ? null : storageService.storeImage(beforeImage, "samples").fileUrl();
        String afterUrl = afterImage == null || afterImage.isEmpty() ? null : storageService.storeImage(afterImage, "samples").fileUrl();
        String finalUrl = finalImage == null || finalImage.isEmpty() ? null : storageService.storeImage(finalImage, "samples").fileUrl();

        if ("final_only".equals(sampleType) && finalUrl == null) {
            throw new IllegalArgumentException("final_only 样本必须上传 finalImage");
        }
        if ("before_after".equals(sampleType) && (beforeUrl == null || afterUrl == null)) {
            throw new IllegalArgumentException("before_after 样本必须上传 beforeImage 和 afterImage");
        }

        Instant now = Instant.now();
        StyleSample sample = new StyleSample(
                store.sampleIds.getAndIncrement(),
                styleId,
                sampleType,
                beforeUrl,
                afterUrl,
                finalUrl,
                sourceType,
                description,
                tags == null ? List.of() : tags,
                null,
                "uploaded",
                now,
                now
        );
        store.samples.put(sample.id(), sample);
        return sample;
    }

    public StyleSample get(Long id) {
        StyleSample sample = store.samples.get(id);
        if (sample == null) {
            throw new NotFoundException("未找到风格样本：" + id);
        }
        return sample;
    }

    public List<StyleSample> list(Long styleId) {
        return store.samples.values()
                .stream()
                .filter(sample -> styleId == null || sample.styleId().equals(styleId))
                .sorted(Comparator.comparing(StyleSample::updatedAt).reversed())
                .toList();
    }

    public StyleSample analyze(Long sampleId) {
        return analyze(sampleId, null);
    }

    public StyleSample analyze(Long sampleId, String provider) {
        StyleSample sample = get(sampleId);
        ColorStyle style = styleService.get(sample.styleId());
        StyleAnalysisResult analysis = AiProviderContext.use(provider, () -> styleAnalysisAgent.analyze(style, sample));
        StyleSample updated = new StyleSample(
                sample.id(),
                sample.styleId(),
                sample.sampleType(),
                sample.beforeImageUrl(),
                sample.afterImageUrl(),
                sample.finalImageUrl(),
                sample.sourceType(),
                sample.description(),
                sample.tags(),
                analysis,
                "analyzed",
                sample.createdAt(),
                Instant.now()
        );
        store.samples.put(sampleId, updated);
        return updated;
    }
}


