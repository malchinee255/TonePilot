package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.service.RagService;
import com.tonepilot.web.dto.RagRetrieveRequest;
import com.tonepilot.web.dto.RagSearchItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/retrieve")
    public ApiResponse<List<RagSearchItem>> retrieve(@RequestBody RagRetrieveRequest request) {
        return ApiResponse.ok(ragService.retrieve(request.query(), request.topK()));
    }
}
