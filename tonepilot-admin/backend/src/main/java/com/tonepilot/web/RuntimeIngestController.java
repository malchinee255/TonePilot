package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.runtime.RuntimeDeviceRegistrationRequest;
import com.tonepilot.runtime.RuntimeDeviceRegistrationResponse;
import com.tonepilot.runtime.RuntimeEventRecord;
import com.tonepilot.runtime.RuntimeEventRequest;
import com.tonepilot.runtime.RuntimeIngestService;
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

    @GetMapping("/events")
    public ApiResponse<List<RuntimeEventRecord>> listEvents(@RequestParam String userId) {
        return ApiResponse.ok(runtimeIngestService.listEvents(userId));
    }
}
