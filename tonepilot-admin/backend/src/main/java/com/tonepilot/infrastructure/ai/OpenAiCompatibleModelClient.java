package com.tonepilot.infrastructure.ai;

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


import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.application.observability.ObservabilityService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OpenAiCompatibleModelClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final LangChainChatModelFactory chatModelFactory;
    private final ObservabilityService observabilityService;

    @Autowired
    public OpenAiCompatibleModelClient(
            AiProperties properties,
            ObjectMapper objectMapper,
            LangChainChatModelFactory chatModelFactory,
            ObservabilityService observabilityService
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.chatModelFactory = chatModelFactory;
        this.observabilityService = observabilityService;
    }

    public String completeJson(String systemPrompt, String userPrompt) {
        String prompt = systemPrompt + "\n\n" + userPrompt;
        Instant startedAt = Instant.now();
        String modelName = safeModelName(false);
        try {
            ChatModel chatModel = chatModelFactory.chatModel(false);
            String content = chatModel.chat(prompt);
            String json = JsonExtractor.object(content);
            observabilityService.recordLlmCall("chat-json", modelName, prompt, json, null, startedAt, Instant.now());
            return json;
        } catch (Exception exception) {
            observabilityService.recordLlmCall("chat-json", modelName, prompt, null, exception, startedAt, Instant.now());
            throw exception;
        }
    }

    public String completeVisionJson(String systemPrompt, String userPrompt, String imageDataUrl) {
        String prompt = systemPrompt + "\n\n" + userPrompt + "\n\n[图片内容已省略，仅记录文本提示]";
        Instant startedAt = Instant.now();
        String modelName = safeModelName(true);
        try {
            ChatModel chatModel = chatModelFactory.chatModel(true);
            String content = chatModel.chat(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(
                            TextContent.from(userPrompt),
                            imageContent(imageDataUrl)
                    )
            ).aiMessage().text();
            String json = JsonExtractor.object(content);
            observabilityService.recordLlmCall("vision-json", modelName, prompt, json, null, startedAt, Instant.now());
            return json;
        } catch (Exception exception) {
            observabilityService.recordLlmCall("vision-json", modelName, prompt, null, exception, startedAt, Instant.now());
            throw exception;
        }
    }

    public <T> T readJson(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (Exception exception) {
            throw new IllegalArgumentException("模型 JSON 解析失败：" + exception.getMessage(), exception);
        }
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("对象序列化失败：" + exception.getMessage(), exception);
        }
    }

    private ImageContent imageContent(String imageDataUrl) {
        if (imageDataUrl == null || imageDataUrl.isBlank()) {
            throw new IllegalArgumentException("图片内容不能为空");
        }
        if (imageDataUrl.startsWith("data:")) {
            int metaEnd = imageDataUrl.indexOf(";base64,");
            if (metaEnd > 5) {
                String mimeType = imageDataUrl.substring("data:".length(), metaEnd);
                String base64Data = imageDataUrl.substring(metaEnd + ";base64,".length());
                return ImageContent.from(base64Data, mimeType);
            }
        }
        return ImageContent.from(imageDataUrl);
    }

    private String safeModelName(boolean vision) {
        try {
            return chatModelFactory.modelName(vision);
        } catch (Exception exception) {
            return properties.activeProvider();
        }
    }
}


