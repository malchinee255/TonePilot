package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.XmpExport;
import com.tonepilot.service.XmpExportService;
import com.tonepilot.web.dto.XmpExportRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/xmp")
public class XmpController {

    private final XmpExportService xmpExportService;

    public XmpController(XmpExportService xmpExportService) {
        this.xmpExportService = xmpExportService;
    }

    @PostMapping("/export")
    public ApiResponse<XmpExport> export(@Valid @RequestBody XmpExportRequest request) {
        return ApiResponse.ok(xmpExportService.export(request));
    }
}
