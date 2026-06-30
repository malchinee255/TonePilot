package com.tonepilot.application.controller;

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







import com.tonepilot.application.dto.ApiResponse;
import com.tonepilot.domain.runtime.RuntimeDeviceRegistrationRequest;
import com.tonepilot.domain.runtime.RuntimeDeviceRegistrationResponse;
import com.tonepilot.domain.runtime.RuntimeEventRecord;
import com.tonepilot.domain.runtime.RuntimeEventRequest;
import com.tonepilot.application.runtime.RuntimeIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/runtime")
public class RuntimeIngestController {

    @Autowired
    private RuntimeIngestService runtimeIngestService;

    @PostMapping("/devices/register")
    public ApiResponse<RuntimeDeviceRegistrationResponse> registerDevice(
            @RequestBody RuntimeDeviceRegistrationRequest request
    ) {
        return ApiResponse.ok(runtimeIngestService.registerDevice(request));
    }

    @PostMapping("/events")
    public ApiResponse<RuntimeEventRecord> recordEvent(@RequestBody RuntimeEventRequest request) {
        return ApiResponse.ok(runtimeIngestService.recordEvent(request));
    }

    @GetMapping("/devices")
    public ApiResponse<List<RuntimeDeviceRecord>> listDevices() {
        return ApiResponse.ok(runtimeIngestService.listDevices());
    }

    @GetMapping("/events")
    public ApiResponse<List<RuntimeEventRecord>> listEvents(
            @RequestParam String userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        return ApiResponse.ok(runtimeIngestService.listEvents(
                new RuntimeEventQuery(userId, sessionId, traceId, eventType, limit)
        ));
    }
}
