package com.tonepilot.runtime.api;

import com.tonepilot.runtime.agent.RuntimeAgentOrchestrator;
import com.tonepilot.runtime.bridge.LightroomStateService;
import com.tonepilot.runtime.config.RuntimeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.util.Map;

@CrossOrigin
@RestController
public class LocalRuntimeController {

    private final LightroomStateService stateService;
    private final RuntimeConfigService configService;
    private final RuntimeAgentOrchestrator orchestrator;

    @Autowired
    public LocalRuntimeController(
            LightroomStateService stateService,
            RuntimeConfigService configService,
            RuntimeAgentOrchestrator orchestrator
    ) {
        this.stateService = stateService;
        this.configService = configService;
        this.orchestrator = orchestrator;
    }

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
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                        <!doctype html>
                        <html lang="zh-CN">
                        <head><meta charset="utf-8"><title>TonePilot Agent</title></head>
                        <body style="margin:0;background:#202020;color:#d6d6d6;font-family:Arial,'Microsoft YaHei',sans-serif;">
                        <main style="display:grid;grid-template-columns:1fr 1fr;height:100vh;">
                          <section style="padding:24px;border-right:1px solid #3a3a3a;">
                            <h2>Lightroom 预览</h2>
                            <pre id="photo">读取中...</pre>
                          </section>
                          <section style="padding:24px;display:flex;flex-direction:column;gap:12px;">
                            <h2>Agent 对话修图</h2>
                            <div id="chat" style="flex:1;overflow:auto;border:1px solid #3a3a3a;padding:12px;"></div>
                            <textarea id="prompt" style="height:84px;background:#2b2b2b;color:#eee;border:1px solid #555;">先分析这张照片，修成夜景电影感，再亮一点</textarea>
                            <button id="send" style="height:40px;background:#4f6680;color:white;border:0;">发送并修图</button>
                          </section>
                        </main>
                        <script>
                        async function poll(){const r=await fetch('/api/lightroom/selected-photo');photo.textContent=JSON.stringify(await r.json(),null,2)}
                        send.onclick=async()=>{chat.innerHTML+='<p><b>你：</b>'+prompt.value+'</p>';const r=await fetch('/api/lightroom-agent/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:prompt.value})});chat.innerHTML+='<pre>'+JSON.stringify(await r.json(),null,2)+'</pre>'}
                        poll();setInterval(poll,2000)
                        </script>
                        </body></html>
                        """);
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
