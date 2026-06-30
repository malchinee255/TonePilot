package com.tonepilot.runtime.application.lightroom;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;

import com.tonepilot.runtime.repository.lightroom.LightroomStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
public class LightroomStateService {

    @Autowired
    private LightroomStateRepository repository;

    public Map<String, Object> status() {
        return repository.status();
    }

    public Map<String, Object> selectedPhoto() {
        return repository.selectedPhoto();
    }

    public Path resultFile(String fileName) {
        return repository.resultFile(fileName);
    }
}
