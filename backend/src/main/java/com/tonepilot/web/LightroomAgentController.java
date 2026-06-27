package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.lightroomagent.LightroomAgentService;
import com.tonepilot.lightroomagent.LightroomAgentTuneRequest;
import com.tonepilot.lightroomagent.LightroomAgentTuneResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/lightroom-agent")
public class LightroomAgentController {

    private final LightroomAgentService lightroomAgentService;

    public LightroomAgentController(LightroomAgentService lightroomAgentService) {
        this.lightroomAgentService = lightroomAgentService;
    }

    @PostMapping("/tune")
    public ApiResponse<LightroomAgentTuneResponse> tune(@Valid @org.springframework.web.bind.annotation.RequestBody LightroomAgentTuneRequest request) {
        return ApiResponse.ok(lightroomAgentService.tune(request));
    }
}
