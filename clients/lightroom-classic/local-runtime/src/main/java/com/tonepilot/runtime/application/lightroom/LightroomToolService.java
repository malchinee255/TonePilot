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

import com.tonepilot.runtime.repository.lightroom.LightroomToolRepository;
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
