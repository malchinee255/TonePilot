package com.tonepilot.infrastructure.knowledge.douyin;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import com.tonepilot.server.dto.DouyinImportRequest;
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
