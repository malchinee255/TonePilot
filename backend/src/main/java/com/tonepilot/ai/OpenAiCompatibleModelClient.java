package com.tonepilot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.observability.ObservabilityService;
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
