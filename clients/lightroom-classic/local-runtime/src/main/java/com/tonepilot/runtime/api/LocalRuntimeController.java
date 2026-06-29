package com.tonepilot.runtime.api;

import com.tonepilot.runtime.agent.RuntimeAgentOrchestrator;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@CrossOrigin
@RestController
public class LocalRuntimeController {

    @Autowired
    private LightroomStateService stateService;

    @Autowired
    private RuntimeConfigService configService;

    @Autowired
    private RuntimeAgentOrchestrator orchestrator;

    @Autowired
    private ResourceLoader resourceLoader;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return stateService.status();
    }

    @GetMapping("/api/lightroom/selected-photo")
    public Map<String, Object> selectedPhoto() {
        return stateService.selectedPhoto();
    }

    @GetMapping("/api/runtime/config")
    public Map<String, Object> runtimeConfig() {
        return configService.readPublicConfig();
    }

    @PostMapping("/api/runtime/config")
    public Map<String, Object> saveRuntimeConfig(@RequestBody Map<String, Object> patch) {
        return configService.writeConfig(patch);
    }

    @PostMapping("/api/lightroom-agent/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> payload) {
        return orchestrator.chat(payload);
    }

    @GetMapping("/agent-console")
    public ResponseEntity<String> agentConsole() {
        try {
            var resource = resourceLoader.getResource("classpath:static/agent-console.html");
            try (var input = resource.getInputStream()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException error) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Agent 控制台加载失败：" + error.getMessage());
        }
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<FileSystemResource> file(@PathVariable String fileName) {
        var path = stateService.bridgePaths().fs("results", fileName).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(path));
    }
}
