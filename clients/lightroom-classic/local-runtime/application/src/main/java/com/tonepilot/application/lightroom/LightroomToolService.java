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




import com.tonepilot.repository.lightroom.LightroomToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LightroomToolService {

    @Autowired
    private LightroomToolRepository repository;

    public Map<String, Object> applyDevelopSettings(Map<String, Object> developSettings) {
        return repository.applyDevelopSettings(developSettings);
    }

    public Map<String, Object> applyStatus(String jobId) {
        return repository.applyStatus(jobId);
    }
}
