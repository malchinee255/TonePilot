package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.service.RagService;
import com.tonepilot.web.dto.RagSearchRequest;
import com.tonepilot.web.dto.RagSearchResponse;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/search")
    public ApiResponse<RagSearchResponse> search(@RequestBody RagSearchRequest request) {
        return ApiResponse.ok(ragService.search(request));
    }
}
