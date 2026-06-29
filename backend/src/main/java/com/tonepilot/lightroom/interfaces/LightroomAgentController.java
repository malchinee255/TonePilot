package com.tonepilot.lightroom.interfaces;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.lightroom.application.LightroomAgentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/lightroom-agent")
public class LightroomAgentController {

    private final LightroomAgentService lightroomAgentService;

    @Autowired
    public LightroomAgentController(LightroomAgentService lightroomAgentService) {
        this.lightroomAgentService = lightroomAgentService;
    }

    @PostMapping("/tune")
    public ApiResponse<LightroomAgentTuneResponse> tune(@Valid @org.springframework.web.bind.annotation.RequestBody LightroomAgentTuneRequest request) {
        return ApiResponse.ok(lightroomAgentService.tune(request));
    }
}


