package com.tonepilot.service;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorAdjustment;
import com.tonepilot.domain.XmpExport;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.XmpExportRequest;
import com.tonepilot.xmp.XmpTemplateRenderer;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class XmpExportService {

    private final InMemoryTonePilotStore store;
    private final ObjectStorageService storageService;
    private final XmpTemplateRenderer renderer = new XmpTemplateRenderer();

    public XmpExportService(InMemoryTonePilotStore store, ObjectStorageService storageService) {
        this.store = store;
        this.storageService = storageService;
    }

    public XmpExport export(XmpExportRequest request) {
        ColorAdjustment adjustment = store.adjustments.get(request.adjustmentId());
        if (adjustment == null) {
            throw new NotFoundException("未找到调色方案：" + request.adjustmentId());
        }
        if (!adjustment.photoId().equals(request.photoId())) {
            throw new IllegalArgumentException("调色方案不属于照片：" + request.photoId());
        }

        String fileName = storageService.slug(request.presetName()) + ".xmp";
        String xmpUrl = storageService.writeTextFile("xmp", fileName, renderer.render(request.presetName(), adjustment));
        XmpExport export = new XmpExport(
                store.xmpExportIds.getAndIncrement(),
                request.photoId(),
                request.adjustmentId(),
                request.presetName(),
                xmpUrl,
                Instant.now()
        );
        store.xmpExports.put(export.id(), export);
        return export;
    }
}
