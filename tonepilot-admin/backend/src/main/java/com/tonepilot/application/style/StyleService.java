package com.tonepilot.application.style;

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


import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.NotFoundException;
import com.tonepilot.domain.style.ColorStyle;
import com.tonepilot.infrastructure.shared.persistence.InMemoryTonePilotStore;
import com.tonepilot.server.dto.StyleRequest;
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


