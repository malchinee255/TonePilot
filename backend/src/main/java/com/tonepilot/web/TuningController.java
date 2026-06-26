package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.lightroom.LightroomConnector;
import com.tonepilot.lightroom.LightroomConnectorStatus;
import com.tonepilot.tuning.TuningMessageRequest;
import com.tonepilot.tuning.TuningSaveRequest;
import com.tonepilot.tuning.TuningSession;
import com.tonepilot.tuning.TuningSessionService;
import com.tonepilot.tuning.TuningStartRequest;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/tuning")
public class TuningController {

    private final TuningSessionService tuningSessionService;
    private final LightroomConnector lightroomConnector;

    public TuningController(TuningSessionService tuningSessionService, LightroomConnector lightroomConnector) {
        this.tuningSessionService = tuningSessionService;
        this.lightroomConnector = lightroomConnector;
    }

    @PostMapping("/sessions")
    public ApiResponse<TuningSession> start(@RequestBody TuningStartRequest request) {
        return ApiResponse.ok(tuningSessionService.start(request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<TuningSession> get(@PathVariable String sessionId) {
        return ApiResponse.ok(tuningSessionService.get(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<TuningSession> sendMessage(
            @PathVariable String sessionId,
            @RequestBody TuningMessageRequest request
    ) {
        return ApiResponse.ok(tuningSessionService.sendMessage(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/save")
    public ApiResponse<TuningSession> save(
            @PathVariable String sessionId,
            @RequestBody(required = false) TuningSaveRequest request
    ) {
        return ApiResponse.ok(tuningSessionService.saveCurrent(sessionId, request));
    }

    @GetMapping("/lightroom/status")
    public ApiResponse<LightroomConnectorStatus> lightroomStatus() {
        return ApiResponse.ok(lightroomConnector.status());
    }
}
