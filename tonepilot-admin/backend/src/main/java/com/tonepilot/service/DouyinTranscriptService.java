package com.tonepilot.service;

import com.tonepilot.web.dto.DouyinImportRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DouyinTranscriptService {

    @Value("${tonepilot.ingestion.douyin.command:}")
    private String command;

    @Value("${tonepilot.ingestion.douyin.timeout-seconds:120}")
    private long timeoutSeconds;

    public String extractTranscript(DouyinImportRequest request) {
        if (command == null || command.isBlank()) {
            return fallbackTranscript(request);
        }
        try {
            Process process = new ProcessBuilder(commandArgs(request))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Duration.ofSeconds(timeoutSeconds).toMillis(), TimeUnit.MILLISECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("抖音字幕提取命令执行超时");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("抖音字幕提取命令失败：" + output);
            }
            return output.isBlank() ? fallbackTranscript(request) : output;
        } catch (Exception exception) {
            throw new IllegalStateException("抖音字幕提取失败：" + exception.getMessage(), exception);
        }
    }

    private List<String> commandArgs(DouyinImportRequest request) {
        String value = command
                .replace("{url}", request.videoUrl())
                .replace("{title}", request.title());
        List<String> args = new ArrayList<>();
        for (String part : value.split("\\s+")) {
            if (!part.isBlank()) {
                args.add(part);
            }
        }
        return args;
    }

    private String fallbackTranscript(DouyinImportRequest request) {
        return """
                抖音视频链接：%s
                视频标题：%s
                作者：%s
                管理员备注：%s

                当前未配置抖音字幕提取命令，系统先根据链接和备注创建可审核素材。
                如果备注中已经包含调色步骤，会进入后续知识抽取。
                """.formatted(
                request.videoUrl(),
                request.title(),
                blankToDefault(request.author(), "未知作者"),
                blankToDefault(request.notes(), "")
        ).trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
