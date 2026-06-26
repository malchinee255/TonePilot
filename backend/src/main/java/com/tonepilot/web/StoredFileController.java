package com.tonepilot.web;

import com.tonepilot.service.ObjectStorageService;
import com.tonepilot.service.StoredObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/files")
@ConditionalOnProperty(prefix = "tonepilot.storage", name = "type", havingValue = "minio")
public class StoredFileController {

    private final ObjectStorageService storageService;

    public StoredFileController(ObjectStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/**")
    public ResponseEntity<byte[]> read(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        StoredObject object = storageService.readObject(path);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(object.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
                .body(object.bytes());
    }
}
