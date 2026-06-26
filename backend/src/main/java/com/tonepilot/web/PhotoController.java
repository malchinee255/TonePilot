package com.tonepilot.web;

import com.tonepilot.common.ApiResponse;
import com.tonepilot.domain.Photo;
import com.tonepilot.domain.PhotoAnalysis;
import com.tonepilot.service.PhotoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Photo> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(photoService.upload(file));
    }

    @GetMapping
    public ApiResponse<List<Photo>> list() {
        return ApiResponse.ok(photoService.list());
    }

    @PostMapping("/{photoId}/analyze")
    public ApiResponse<PhotoAnalysis> analyze(
            @PathVariable Long photoId,
            @RequestParam(required = false) String provider
    ) {
        return ApiResponse.ok(photoService.analyze(photoId, provider));
    }
}
