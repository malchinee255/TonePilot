package com.tonepilot.application.lightroom;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.config.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.lightroom.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.repository.lightroom.*;
import com.tonepilot.infrastructure.admin.*;
import com.tonepilot.infrastructure.config.*;
import com.tonepilot.infrastructure.lightroom.filesystem.*;
import com.tonepilot.infrastructure.lightroom.repository.*;
import com.tonepilot.infrastructure.model.*;
import com.tonepilot.infrastructure.observability.*;




import com.tonepilot.repository.lightroom.LightroomStateRepository;
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
