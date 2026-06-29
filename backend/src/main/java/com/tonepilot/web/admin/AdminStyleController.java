package com.tonepilot.web.admin;

import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.ColorStyle;
import com.tonepilot.service.StyleService;
import com.tonepilot.web.dto.StyleRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/admin/styles")
public class AdminStyleController {

    private final StyleService styleService;

    @Autowired
    public AdminStyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @PostMapping
    public ApiResponse<ColorStyle> create(@Valid @RequestBody StyleRequest request) {
        return ApiResponse.ok(styleService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ColorStyle>> list() {
        return ApiResponse.ok(styleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ColorStyle> get(@PathVariable Long id) {
        return ApiResponse.ok(styleService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ColorStyle> update(@PathVariable Long id, @Valid @RequestBody StyleRequest request) {
        return ApiResponse.ok(styleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        styleService.delete(id);
        return ApiResponse.ok();
    }
}


