package com.tonepilot.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.ColorStyle;
import com.tonepilot.store.InMemoryTonePilotStore;
import com.tonepilot.web.dto.StyleRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class StyleService {

    private final InMemoryTonePilotStore store;

    @Autowired
    public StyleService(InMemoryTonePilotStore store) {
        this.store = store;
    }

    public ColorStyle create(StyleRequest request) {
        Instant now = Instant.now();
        ColorStyle style = new ColorStyle(
                store.styleIds.getAndIncrement(),
                request.styleName(),
                request.styleCode(),
                request.description(),
                request.suitableScenes() == null ? List.of() : request.suitableScenes(),
                request.avoidScenes() == null ? List.of() : request.avoidScenes(),
                request.status() == null || request.status().isBlank() ? "enabled" : request.status(),
                now,
                now
        );
        store.styles.put(style.id(), style);
        return style;
    }

    public List<ColorStyle> list() {
        return store.styles.values()
                .stream()
                .sorted(Comparator.comparing(ColorStyle::updatedAt).reversed())
                .toList();
    }

    public ColorStyle get(Long id) {
        ColorStyle style = store.styles.get(id);
        if (style == null) {
            throw new NotFoundException("未找到调色风格：" + id);
        }
        return style;
    }

    public ColorStyle update(Long id, StyleRequest request) {
        ColorStyle existing = get(id);
        ColorStyle updated = new ColorStyle(
                existing.id(),
                request.styleName(),
                request.styleCode(),
                request.description(),
                request.suitableScenes() == null ? List.of() : request.suitableScenes(),
                request.avoidScenes() == null ? List.of() : request.avoidScenes(),
                request.status() == null || request.status().isBlank() ? existing.status() : request.status(),
                existing.createdAt(),
                Instant.now()
        );
        store.styles.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        if (store.styles.remove(id) == null) {
            throw new NotFoundException("未找到调色风格：" + id);
        }
    }
}


